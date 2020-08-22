/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v1.core

import android.os.Build.VERSION
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kohii.v1.core.Strategy.MULTI_PLAYER
import kohii.v1.core.Strategy.NO_PLAYER
import kohii.v1.findCoordinatorLayoutDirectChildContainer
import kohii.v1.internal.BehaviorWrapper
import kohii.v1.internal.NestedScrollViewBucket
import kohii.v1.internal.RecyclerViewBucket
import kohii.v1.internal.ViewGroupBucket
import kohii.v1.internal.ViewGroupV23Bucket
import kohii.v1.internal.ViewPager2Bucket
import kohii.v1.internal.ViewPagerBucket
import kohii.v1.logInfo
import kohii.v1.media.VolumeInfo
import kotlin.LazyThreadSafetyMode.NONE

abstract class Bucket constructor(
  val manager: Manager,
  open val root: View,
  strategy: Strategy,
  internal val selector: Selector
) : OnLayoutChangeListener {

  companion object {
    const val VERTICAL = RecyclerView.VERTICAL
    const val HORIZONTAL = RecyclerView.HORIZONTAL
    const val BOTH_AXIS = -1
    const val NONE_AXIS = -2

    private val playbackComparators = mapOf(
        HORIZONTAL to Playback.HORIZONTAL_COMPARATOR,
        VERTICAL to Playback.VERTICAL_COMPARATOR,
        BOTH_AXIS to Playback.BOTH_AXIS_COMPARATOR,
        NONE_AXIS to Playback.BOTH_AXIS_COMPARATOR
    )

    @JvmStatic
    internal operator fun get(
      manager: Manager,
      root: View,
      strategy: Strategy,
      selector: Selector
    ): Bucket {
      return when (root) {
        is RecyclerView -> RecyclerViewBucket(manager, root, strategy, selector)
        is NestedScrollView -> NestedScrollViewBucket(manager, root, strategy, selector)
        is ViewPager2 -> ViewPager2Bucket(manager, root, strategy, selector)
        is ViewPager -> ViewPagerBucket(manager, root, strategy, selector)
        is ViewGroup -> {
          if (VERSION.SDK_INT >= 23)
            ViewGroupV23Bucket(manager, root, strategy, selector)
          else
            ViewGroupBucket(manager, root, strategy, selector)
        }
        else -> throw IllegalArgumentException("Unsupported: $root")
      }
    }
  }

  private val containerAttachStateChangeListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) {
      "Bucket# container is attached: $v, $this".logInfo()
      manager.onContainerAttachedToWindow(v)
    }

    override fun onViewDetachedFromWindow(v: View?) {
      "Bucket# container is detached: $v, $this".logInfo()
      manager.onContainerDetachedFromWindow(v)
    }
  }

  private val rootAttachStateChangeListener = object : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View?) {
      onAttached()
    }

    override fun onViewDetachedFromWindow(v: View?) {
      onDetached()
    }
  }

  private val containers = mutableSetOf<Any>()

  private val behaviorHolder = lazy(NONE) {
    val container = manager.group.activity.window.peekDecorView()
        ?.findCoordinatorLayoutDirectChildContainer(root)
    val params = container?.layoutParams
    return@lazy if (params is CoordinatorLayout.LayoutParams) params else null
  }

  private val volumeConstraint: VolumeInfo
    get() = when (strategy) {
      MULTI_PLAYER -> VolumeInfo.DEFAULT_INACTIVE
      else -> VolumeInfo.DEFAULT_ACTIVE
    }

  internal val volumeInfo: VolumeInfo
    get() = bucketVolumeInfo

  internal var bucketVolumeInfo: VolumeInfo = VolumeInfo.DEFAULT_ACTIVE
    set(value) {
      field = value
      manager.onBucketVolumeInfoUpdated(this, effectiveVolumeInfo(this.volumeInfo))
    }

  internal var strategy: Strategy = strategy
    set(value) {
      val from = field
      field = value
      val to = field
      if (from !== to) {
        manager.onBucketVolumeInfoUpdated(this, effectiveVolumeInfo(this.volumeInfo))
        manager.refresh()
      }
    }

  internal var lock: Boolean = manager.lock
    get() = field || manager.lock
    set(value) {
      field = value
      manager.playbacks
          .filter { it.value.bucket === this }
          .forEach { it.value.lock = value }
      manager.refresh()
    }

  init {
    bucketVolumeInfo = manager.volumeInfo
  }

  abstract fun accepts(container: ViewGroup): Boolean

  open fun allowToPlay(playback: Playback): Boolean = playback.token.shouldPlay()

  abstract fun selectToPlay(candidates: Collection<Playback>): Collection<Playback>

  @CallSuper
  open fun addContainer(container: ViewGroup) {
    if (containers.add(container)) {
      if (container.isAttachedToWindow) {
        containerAttachStateChangeListener.onViewAttachedToWindow(container)
      }
      container.addOnAttachStateChangeListener(
          containerAttachStateChangeListener
      )
    }
  }

  @CallSuper
  open fun removeContainer(container: ViewGroup) {
    if (containers.remove(container)) {
      container.removeOnAttachStateChangeListener(
          containerAttachStateChangeListener
      )
      container.removeOnLayoutChangeListener(this)
    }
  }

  @CallSuper
  override fun onLayoutChange(
    v: View?,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if (v != null && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
      manager.onContainerLayoutChanged(v)
    }
  }

  @CallSuper
  open fun onAdded() {
    "Bucket is added: $this".logInfo()
    if (root.isAttachedToWindow) {
      rootAttachStateChangeListener.onViewAttachedToWindow(root)
    }
    root.addOnAttachStateChangeListener(rootAttachStateChangeListener)
  }

  @CallSuper
  open fun onAttached() {
    "Bucket is attached: $this".logInfo()
    behaviorHolder.value?.let {
      val behavior = it.behavior
      if (behavior != null) {
        val behaviorWrapper = BehaviorWrapper(behavior, manager)
        it.behavior = behaviorWrapper
      }
    }
  }

  @CallSuper
  open fun onDetached() {
    "Bucket is detached: $this".logInfo()
    if (behaviorHolder.isInitialized()) {
      behaviorHolder.value?.let {
        val behavior = it.behavior
        if (behavior is BehaviorWrapper) {
          behavior.onDetach()
          it.behavior = behavior.delegate
        }
      }
    }
  }

  @CallSuper
  open fun onRemoved() {
    "Bucket is removed: $this".logInfo()
    root.removeOnAttachStateChangeListener(rootAttachStateChangeListener)
    mutableListOf(containers)
        .onEach(manager::onRemoveContainer)
        .clear()
  }

  internal fun effectiveVolumeInfo(origin: VolumeInfo): VolumeInfo {
    return with(origin) {
      val constraint = volumeConstraint
      VolumeInfo(mute || constraint.mute, volume.coerceAtMost(constraint.volume))
    }
  }

  // This operation should be considered heavy/expensive.
  protected fun selectByOrientation(
    candidates: Collection<Playback>,
    orientation: Int
  ): Collection<Playback> {
    if (lock) return emptyList()
    if (strategy == NO_PLAYER) return emptyList()

    val playbackComparator = playbackComparators.getValue(orientation)
    val manualToAutoPlaybackGroups = candidates
        .sortedWith(playbackComparator)
        .groupBy { it.tag != Master.NO_TAG && it.config.controller != null }
        .withDefault { emptyList() }

    val manualCandidate = with(manualToAutoPlaybackGroups.getValue(true)) {
      val started = find { manager.master.manuallyStartedPlayable.get() === it.playable }
      listOfNotNull(started ?: firstOrNull())
    }

    return if (manualCandidate.isNotEmpty()) {
      manualCandidate
    } else {
      selector(manualToAutoPlaybackGroups.getValue(false))
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Bucket
    if (manager !== other.manager) return false
    if (root !== other.root) return false
    return true
  }

  private var lazyHashCode: Int = -1

  override fun hashCode(): Int {
    if (lazyHashCode == -1) {
      lazyHashCode = manager.hashCode() * 31 + root.hashCode()
    }
    return lazyHashCode
  }
}
