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
import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import kohii.v1.Playback
import kohii.v1.PlaybackManager

internal class NestedScrollViewContainer(
  override val container: NestedScrollView,
  manager: PlaybackManager
) : ViewContainer<NestedScrollView>(container, manager), OnScrollChangeListener {

  override fun onAdded() {
    super.onAdded()
    container.setOnScrollChangeListener(this)
  }

  override fun onRemoved() {
    super.onRemoved()
    container.setOnScrollChangeListener(null as OnScrollChangeListener?)
  }

  override fun onScrollChange(
    v: NestedScrollView?,
    scrollX: Int,
    scrollY: Int,
    oldScrollX: Int,
    oldScrollY: Int
  ) {
    manager.dispatchRefreshAll()
  }

  override fun allowsToPlay(playback: Playback<*, *>): Boolean {
    return playback.token.shouldPlay()
  }

  override fun accepts(target: Any): Boolean {
    if (target !is View) return false
    var view = target
    var parent = view.parent
    while (parent != null && parent !== this.container && parent is View) {
      @Suppress("USELESS_CAST")
      view = parent as View
      parent = view.parent
    }
    return parent === this.container
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NestedScrollViewContainer) return false
    if (container !== other.container) return false
    return true
  }

  override fun hashCode(): Int {
    return container.hashCode()
  }
}
