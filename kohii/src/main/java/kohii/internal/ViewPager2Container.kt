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

import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kohii.v1.Container
import kohii.v1.Playback
import kohii.v1.PlaybackManager

class ViewPager2Container(
  private val container: ViewPager2,
  private val manager: PlaybackManager
) : Container, OnPageChangeCallback() {

  override fun onHostAttached() {
    container.registerOnPageChangeCallback(this)
    manager.dispatchRefreshAll()
  }

  override fun onHostDetached() {
    container.unregisterOnPageChangeCallback(this)
  }

  override fun onPageScrollStateChanged(state: Int) {
    manager.dispatchRefreshAll()
  }

  override fun onPageSelected(position: Int) {
    manager.dispatchRefreshAll()
  }

  override fun allowsToPlay(playback: Playback<*>): Boolean {
    return playback.token?.shouldPlay() == true
  }

  override fun accepts(target: Any): Boolean {
    return false
  }

  override fun toString(): String {
    return "${container.javaClass.simpleName}::${Integer.toHexString(hashCode())}"
  }

}