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
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import kohii.v1.Playback
import kohii.v1.PlaybackManager

class ViewPagerContainer(
  override val container: ViewPager,
  manager: PlaybackManager
) : ViewContainer<ViewPager>(container, manager), OnPageChangeListener {

  override fun onManagerAttached() {
    container.addOnPageChangeListener(this)
    manager.dispatchRefreshAll()
  }

  override fun onManagerDetached() {
    container.removeOnPageChangeListener(this)
  }

  override fun onPageScrollStateChanged(state: Int) {
    manager.dispatchRefreshAll()
  }

  override fun onPageSelected(position: Int) {
    manager.dispatchRefreshAll()
  }

  override fun onPageScrolled(
    position: Int,
    positionOffset: Float,
    positionOffsetPixels: Int
  ) {
    // no-op
  }

  override fun allowsToPlay(playback: Playback<*>): Boolean {
    return playback.token.shouldPlay()
  }

  override fun accepts(target: Any): Boolean {
    return if (target is View) {
      var view = target
      var parent = view.parent
      while (parent != null && parent !== this.container && parent is View) {
        @Suppress("USELESS_CAST")
        view = parent as View
        parent = view.parent
      }
      parent === this.container
    } else false
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ViewPagerContainer) return false
    if (container != other.container) return false
    return true
  }

  override fun hashCode(): Int {
    return container.hashCode()
  }

}