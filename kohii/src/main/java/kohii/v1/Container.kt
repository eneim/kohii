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

package kohii.v1

import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kohii.internal.NestedScrollViewContainer
import kohii.internal.RecyclerViewContainer
import kohii.internal.ViewGroupContainerBase
import kohii.internal.ViewGroupContainerV23
import kohii.internal.ViewPager2Container
import kohii.internal.ViewPagerContainer

interface Container {

  companion object {
    internal fun createContainer(
      kohii: Kohii,
      view: Any,
      manager: PlaybackManager
    ): Container? {
      return when (view) {
        is RecyclerView ->
          RecyclerViewContainer(view, manager)
        is NestedScrollView ->
          NestedScrollViewContainer(view, manager)
        is ViewPager ->
          ViewPagerContainer(view, manager)
        is ViewPager2 ->
          ViewPager2Container(view, manager)
        is ViewGroup ->
          if (Build.VERSION.SDK_INT >= 23) ViewGroupContainerV23(
              view, manager
          ) else ViewGroupContainerBase(view, manager)
        else -> null
      }
    }
  }

  val container: Any

  // Call when the PlaybackManager is attached.
  fun onHostAttached() {

  }

  fun onHostDetached() {

  }

  // Must contain and allow it to play.
  fun allowsToPlay(playback: Playback<*>): Boolean

  fun accepts(target: Any): Boolean

  fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
  }

}