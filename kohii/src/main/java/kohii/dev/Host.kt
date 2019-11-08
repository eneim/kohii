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

package kohii.dev

import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kohii.logError
import kotlin.LazyThreadSafetyMode.NONE

abstract class Host<V : View> constructor(
  val manager: Manager,
  val root: V
) : OnAttachStateChangeListener, OnLayoutChangeListener {

  companion object {
    const val VERTICAL = RecyclerView.VERTICAL
    const val HORIZONTAL = RecyclerView.HORIZONTAL
    const val BOTH_AXIS = -1
    const val NONE_AXIS = -2

    val comparators = mapOf(
        HORIZONTAL to Playback.HORIZONTAL_COMPARATOR,
        VERTICAL to Playback.VERTICAL_COMPARATOR,
        BOTH_AXIS to Playback.BOTH_AXIS_COMPARATOR,
        NONE_AXIS to Playback.BOTH_AXIS_COMPARATOR
    )

    internal operator fun get(
      manager: Manager,
      root: View
    ): Host<*> {
      return when (root) {
        is RecyclerView -> RecyclerViewHost(manager, root)
        is NestedScrollView -> NestedScrollViewHost(manager, root)
        is ViewPager2 -> ViewPager2Host(manager, root)
        is ViewPager -> ViewPagerHost(manager, root)
        is ViewGroup -> {
          if (Build.VERSION.SDK_INT >= 23)
            ViewGroupV23Host(manager, root)
          else
            ViewGroupHost(manager, root)
        }
        else -> throw IllegalArgumentException("Unsupported: $root")
      }
    }
  }

  private val containers = mutableSetOf<Any>()

  abstract fun accepts(container: ViewGroup): Boolean

  abstract fun allowToPlay(playback: Playback<*>): Boolean

  abstract fun selectToPlay(
    candidates: Collection<Playback<*>>,
    all: Collection<Playback<*>>
  ): Collection<Playback<*>>

  @CallSuper
  open fun addContainer(container: ViewGroup) {
    if (containers.add(container)) {
      if (ViewCompat.isAttachedToWindow(container)) {
        this.onViewAttachedToWindow(container)
      }
      container.addOnAttachStateChangeListener(this)
    }
  }

  @CallSuper
  open fun removeContainer(container: ViewGroup) {
    if (containers.remove(container)) {
      container.removeOnAttachStateChangeListener(this)
      container.removeOnLayoutChangeListener(this)
    }
  }

  @CallSuper
  override fun onViewDetachedFromWindow(v: View?) {
    manager.onContainerDetachedFromWindow(v)
  }

  @CallSuper
  override fun onViewAttachedToWindow(v: View?) {
    manager.onContainerAttachedToWindow(v)
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
    if (v != null && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
      manager.onContainerLayoutChanged(v)
    }
  }

  @CallSuper
  open fun onAdded() {
  }

  @CallSuper
  open fun onRemoved() {
    mutableListOf(containers).onEach {
      manager.onRemoveContainer(it)
    }
        .clear()
  }

  // This operation should be considered heavy/expensive.
  protected fun selectByOrientation(
    candidates: Collection<Playback<*>>,
    all: Collection<Playback<*>> = candidates,
    orientation: Int
  ): Collection<Playback<*>> {
    val comparator = comparators.getValue(orientation)

    val grouped = candidates.sortedWith(comparator)
        .groupBy { it.config.controller != null }
        .withDefault { emptyList() }

    val manualCandidates by lazy(NONE) {
      val manual = grouped.getValue(true)
      val started =
        if (manual.isNotEmpty()) {
          manual.asSequence()
              .firstOrNull { playback ->
                val playable = manager.findPlayableForContainer(playback.container)
                manager.master.playablesPendingStates
                    .filter { it.key === playable?.tag }
                    .isNotEmpty()
              }
        } else null
      return@lazy listOfNotNull(started ?: manual.firstOrNull())
    }

    val automaticCandidates by lazy(NONE) {
      listOfNotNull(grouped.getValue(false).firstOrNull())
    }

    val result = if (manualCandidates.isNotEmpty()) manualCandidates else automaticCandidates

    result.forEach { it.distanceToPlay = 0 }
    all.partition { it.isActive }
        .also { (active, inactive) ->
          inactive.forEach { it.distanceToPlay = Int.MAX_VALUE }
          active.sortedWith(comparator)
              .also {
                val (start, end) =
                  it.indexOf(result.firstOrNull()) to it.indexOf(result.lastOrNull())
                if (start > 0) {
                  for (i in 0 until start) it[i].distanceToPlay = start - i
                }
                if (end < it.size - 1) {
                  for (i in end + 1 until it.size) it[i].distanceToPlay = i - end
                }
              }
        }
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Host<*>
    if (manager !== other.manager) return false
    if (root !== other.root) return false
    return true
  }

  override fun hashCode(): Int {
    var result = manager.hashCode()
    result = 31 * result + root.hashCode()
    return result
  }
}
