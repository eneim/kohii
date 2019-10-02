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

package kohii.internal

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import kohii.doOnAttach
import kohii.doOnDetach
import kohii.findSuitableParent
import kohii.media.VolumeInfo
import kohii.v1.Playback
import kohii.v1.PlaybackManager
import kohii.v1.TargetHost
import kotlin.LazyThreadSafetyMode.NONE

abstract class BaseTargetHost<V : Any>(
  protected val actualHost: V,
  manager: PlaybackManager,
  selector: Selector? = null
) : TargetHost(actualHost, manager, selector),
    View.OnAttachStateChangeListener,
    View.OnLayoutChangeListener {

  private val targets = HashMap<Any, Any>()

  override var lock = false
  override var volumeInfo: VolumeInfo = VolumeInfo()

  private val lazyHashCode by lazy(NONE) {
    var result = host.hashCode()
    result = 31 * result + manager.hashCode()
    result
  }

  override fun onAdded() {
    super.onAdded()
    (host as? View)?.apply {
      doOnAttach {
        val foundParent = findSuitableParent(manager.parent.activity.window.peekDecorView(), it)
        val param = foundParent?.layoutParams as? CoordinatorLayout.LayoutParams
        if (param != null && param.behavior is ScrollingViewBehavior) {
          val behaviorWrapper = BehaviorWrapper(param.behavior!!, manager)
          param.behavior = behaviorWrapper
        }
      }

      doOnDetach {
        val foundParent = findSuitableParent(manager.parent.activity.window.peekDecorView(), it)
        val param = foundParent?.layoutParams as? CoordinatorLayout.LayoutParams
        if (param != null && param.behavior is BehaviorWrapper) {
          (param.behavior as BehaviorWrapper).onDetach()
        }
      }
    }
  }

  override fun onRemoved() {
    this.targets.clear()
  }

  override fun <T : Any> attachTarget(target: T) {
    if (targets.put(target, PRESENT) == null) { // null --> no previous map
      if (target is View) {
        if (ViewCompat.isAttachedToWindow(target)) {
          this.onViewAttachedToWindow(target)
        }
        target.addOnAttachStateChangeListener(this)
      }
    }
  }

  override fun <T : Any> detachTarget(target: T) {
    if (targets.remove(target) != null) { // non-null --> was mapped with a value
      if (target is View) {
        target.removeOnAttachStateChangeListener(this)
        target.removeOnLayoutChangeListener(this)
      }
    }
  }

  override fun onViewAttachedToWindow(v: View?) {
    if (v != null) {
      manager.onTargetAttached(v)
      v.addOnLayoutChangeListener(this)
    }
  }

  override fun onViewDetachedFromWindow(v: View?) {
    if (v != null) {
      v.removeOnLayoutChangeListener(this)
      manager.onTargetDetached(v)
    }
  }

  protected open fun selectByOrientation(
    candidates: Collection<Playback<*>>,
    orientation: Int
  ): Collection<Playback<*>> {
    val grouped = candidates.filter { !it.config.disabled() } // ignore those are disabled.
        .groupBy { it.controller != null }
        .withDefault { emptyList() }

    val manualCandidate by lazy {
      val sorted = grouped.getValue(true)
          .sortedWith(comparators.getValue(orientation))
      val manuallyStarted =
        sorted.firstOrNull { playback -> manager.kohii.manualPlayableRecord[playback.playable] == true }
      return@lazy listOfNotNull(manuallyStarted ?: sorted.firstOrNull())
    }

    val automaticCandidate by lazy {
      listOfNotNull(
          grouped.getValue(false).sortedWith(comparators.getValue(orientation)).firstOrNull()
      )
    }

    return if (manualCandidate.isNotEmpty()) manualCandidate else automaticCandidate
  }

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
    if (v != null && changed(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
      manager.onTargetUpdated(v)
    }
  }

  override fun toString(): String {
    return "${host.javaClass.simpleName}::${Integer.toHexString(hashCode())}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BaseTargetHost<*>) return false

    // Compare references on purpose
    if (host !== other.host) return false
    if (manager !== other.manager) return false

    return true
  }

  override fun hashCode(): Int {
    return lazyHashCode
  }
}
