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
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kohii.v1.Playback
import kohii.v1.PlaybackManager
import java.lang.ref.WeakReference

internal class ViewPager2TargetHost(
  host: ViewPager2,
  manager: PlaybackManager,
  selector: Selector? = null
) : BaseTargetHost<ViewPager2>(host, manager, selector) {

  private val pageChangeCallback by lazy { SimpleOnPageChangeCallback(manager) }

  override fun onAdded() {
    super.onAdded()
    actualHost.registerOnPageChangeCallback(pageChangeCallback)
    manager.dispatchRefreshAll()
  }

  override fun onRemoved() {
    super.onRemoved()
    actualHost.unregisterOnPageChangeCallback(pageChangeCallback)
  }

  override fun allowsToPlay(playback: Playback<*>): Boolean {
    return playback.token.shouldPlay()
  }

  override fun accepts(target: Any): Boolean {
    return if (target is View) {
      var view = target
      var parent = view.parent
      while (parent != null && parent !== this.host && parent is View) {
        @Suppress("USELESS_CAST")
        view = parent as View
        parent = view.parent
      }
      parent === this.host
    } else false
  }

  override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    if (selector != null) return selector.select(candidates)
    return super.selectByOrientation(candidates, actualHost.orientation)
  }

  private class SimpleOnPageChangeCallback(manager: PlaybackManager) : OnPageChangeCallback() {
    val weakManager = WeakReference(manager)

    override fun onPageSelected(position: Int) {
      weakManager.get()
          ?.dispatchRefreshAll()
    }

    override fun onPageScrollStateChanged(state: Int) {
      weakManager.get()
          ?.dispatchRefreshAll()
    }

    override fun onPageScrolled(
      position: Int,
      positionOffset: Float,
      positionOffsetPixels: Int
    ) {
      weakManager.get()
          ?.dispatchRefreshAll()
    }
  }
}
