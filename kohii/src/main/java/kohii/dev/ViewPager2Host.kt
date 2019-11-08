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

import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import java.lang.ref.WeakReference
import kotlin.LazyThreadSafetyMode.NONE

class ViewPager2Host(
  manager: Manager,
  root: ViewPager2
) : Host<ViewPager2>(manager, root) {

  private class SimplePageChangeCallback(manager: Manager) : ViewPager2.OnPageChangeCallback() {
    val weakManager = WeakReference(manager)

    override fun onPageScrollStateChanged(state: Int) {
      weakManager.get()
          ?.refresh()
    }

    override fun onPageSelected(position: Int) {
      weakManager.get()
          ?.refresh()
    }
  }

  private val pageChangeCallback by lazy(NONE) { SimplePageChangeCallback(manager) }

  override fun onAdded() {
    super.onAdded()
    root.registerOnPageChangeCallback(pageChangeCallback)
  }

  override fun onRemoved() {
    super.onRemoved()
    root.unregisterOnPageChangeCallback(pageChangeCallback)
  }

  override fun accepts(container: ViewGroup): Boolean {
    var view = container as View
    var parent = view.parent
    while (parent != null && parent !== this.root && parent is View) {
      view = parent
      parent = view.parent
    }
    return parent === this.root
  }

  override fun allowToPlay(playback: Playback<*>): Boolean {
    return playback.token.shouldPlay()
  }

  override fun selectToPlay(
    candidates: Collection<Playback<*>>,
    all: Collection<Playback<*>>
  ): Collection<Playback<*>> {
    return selectByOrientation(candidates, all, orientation = root.orientation)
  }
}
