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
import androidx.recyclerview.widget.RecycleViewUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kohii.v1.Container
import kohii.v1.Playback
import kohii.v1.PlaybackManager

internal data class RecyclerViewContainer(
  override val container: RecyclerView,
  private val manager: PlaybackManager
) : Container, OnScrollListener() {

  override fun onHostAttached() {
    val params = container.layoutParams
    @Suppress("UNUSED_VARIABLE")
    val behavior = (params as? CoordinatorLayout.LayoutParams)?.behavior
    // TODO deal with CoordinatorLayout?
    container.addOnScrollListener(this)
    if (container.scrollState == SCROLL_STATE_IDLE) manager.dispatchRefreshAll()
  }

  override fun onHostDetached() {
    container.removeOnScrollListener(this)
  }

  override fun onScrolled(
    recyclerView: RecyclerView,
    dx: Int,
    dy: Int
  ) {
    manager.dispatchRefreshAll()
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

  override fun toString(): String {
    return "${container.javaClass.simpleName}::${Integer.toHexString(hashCode())}"
  }

}