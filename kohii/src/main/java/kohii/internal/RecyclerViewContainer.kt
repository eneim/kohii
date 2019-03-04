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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecycleViewUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kohii.v1.Container.Companion.BOTH_AXIS
import kohii.v1.Container.Companion.HORIZONTAL
import kohii.v1.Container.Companion.NONE_AXIS
import kohii.v1.Container.Companion.VERTICAL
import kohii.v1.Container.Companion.comparators
import kohii.v1.Playback
import kohii.v1.PlaybackManager
import java.lang.ref.WeakReference

internal class RecyclerViewContainer(
  override val container: RecyclerView,
  manager: PlaybackManager
) : ViewContainer<RecyclerView>(container, manager) {

  companion object {
    fun RecyclerView.fetchOrientation(): Int {
      val layout = this.layoutManager ?: return NONE_AXIS
      return when (layout) {
        is LinearLayoutManager -> layout.orientation
        is StaggeredGridLayoutManager -> layout.orientation
        else -> {
          return if (layout.canScrollVertically()) {
            if (layout.canScrollHorizontally()) BOTH_AXIS
            else VERTICAL
          } else {
            if (layout.canScrollHorizontally()) HORIZONTAL
            else NONE_AXIS
          }
        }
      }
    }
  }

  private val scrollListener by lazy { SimpleOnScrollListener(manager) }

  override fun onManagerAttached() {
    // TODO deal with CoordinatorLayout?
    val params = container.layoutParams
    @Suppress("UNUSED_VARIABLE")
    val behavior = (params as? CoordinatorLayout.LayoutParams)?.behavior
    container.addOnScrollListener(scrollListener)
    if (container.scrollState == SCROLL_STATE_IDLE) manager.dispatchRefreshAll()
  }

  override fun onManagerDetached() {
    container.removeOnScrollListener(scrollListener)
  }

  override fun allowsToPlay(playback: Playback<*>): Boolean {
    return playback.target is View &&
        this.container.findContainingViewHolder(playback.target) != null
        && playback.token?.shouldPlay() == true
  }

  override fun accepts(target: Any): Boolean {
    if (target !is View) return false
    val params = RecycleViewUtils.fetchItemViewParams(target)
    return RecycleViewUtils.checkParams(container, params)
  }

  override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    val orientation = container.fetchOrientation()
    return candidates.sortedWith(comparators.getValue(orientation))
        .let {
          if (it.isEmpty()) emptyList() else listOf(it.first())
        }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RecyclerViewContainer) return false
    if (container !== other.container) return false
    return true
  }

  override fun hashCode(): Int {
    return container.hashCode()
  }

  private class SimpleOnScrollListener(manager: PlaybackManager) : OnScrollListener() {
    val weakManager = WeakReference(manager)

    override fun onScrolled(
      recyclerView: RecyclerView,
      dx: Int,
      dy: Int
    ) {
      weakManager.get()
          ?.dispatchRefreshAll()
    }
  }
}
