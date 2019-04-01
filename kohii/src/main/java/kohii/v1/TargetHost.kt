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
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kohii.internal.NestedScrollViewTargetHost
import kohii.internal.RecyclerViewTargetHost
import kohii.internal.ViewGroupTargetHostBase
import kohii.internal.ViewGroupTargetHostV23
import kohii.internal.ViewPager2TargetHost
import kohii.internal.ViewPagerTargetHost
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo

/**
 * A TargetHost is the representation of a View in Kohii. A TargetHost wraps a View and provides it
 * internal support such as listening to Scroll event, wrapping it with special LayoutParam to provide more control, etc.
 *
 * Implementation of a TargetHost must correctly override required methods, such as [select], [accepts], ...
 * The [PlaybackManager] will talk to TargetHost to ask for necessary information to update the overall behavior.
 */
interface TargetHost : Comparable<TargetHost> {

  companion object {
    internal const val VERTICAL = RecyclerView.VERTICAL
    internal const val HORIZONTAL = RecyclerView.HORIZONTAL
    internal const val BOTH_AXIS = -1
    internal const val NONE_AXIS = -2

    // read-only map
    val comparators = listOf(
        Pair(HORIZONTAL, Playback.HORIZONTAL_COMPARATOR),
        Pair(VERTICAL, Playback.VERTICAL_COMPARATOR),
        Pair(BOTH_AXIS, Playback.BOTH_AXIS_COMPARATOR),
        Pair(NONE_AXIS, Playback.BOTH_AXIS_COMPARATOR)
    ).toMap()

    internal fun createTargetHost(
      view: Any,
      manager: PlaybackManager
    ): TargetHost? {
      return when (view) {
        is RecyclerView ->
          RecyclerViewTargetHost(view, manager)
        is NestedScrollView ->
          NestedScrollViewTargetHost(view, manager)
        is ViewPager ->
          ViewPagerTargetHost(view, manager)
        is ViewPager2 ->
          ViewPager2TargetHost(view, manager)
        is ViewGroup ->
          if (Build.VERSION.SDK_INT >= 23) ViewGroupTargetHostV23(view, manager)
          else ViewGroupTargetHostBase(view, manager)
        else -> null
      }
    }

    val PRESENT = Any()

    internal fun changed(
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      oldLeft: Int,
      oldTop: Int,
      oldRight: Int,
      oldBottom: Int
    ): Boolean {
      return top != oldTop || bottom != oldBottom || left != oldLeft || right != oldRight
    }
  }

  override fun compareTo(other: TargetHost): Int {
    return 0 // all are equal by default.
  }

  // The ViewGroup
  val host: Any

  var volumeInfo: VolumeInfo

  fun onAdded() {}

  fun onRemoved() {}

  // Call when the PlaybackManager is attached.
  fun onManagerAttached() {}

  // Call when the PlaybackManager is detached.
  fun onManagerDetached() {}

  fun <T> attachTarget(target: T)

  fun <T> detachTarget(target: T)

  // Called by Manager when creating Playback object for the Target.
  fun accepts(target: Any): Boolean

  // Must contain and allow it to play.
  fun allowsToPlay(playback: Playback<*, *>): Boolean

  fun select(candidates: Collection<Playback<*, *>>): Collection<Playback<*, *>> {
    return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
  }

  // Update PlaybackInfo for TargetHost-scoped
  // Ensure that any newly added Playback has the same init state as this info.
  fun applyPlaybackInfo(playbackInfo: PlaybackInfo) {
    // TODO("Implement this")
  }

  fun applyVolumeInfo(volumeInfo: VolumeInfo) {
    // TODO("Implement this")
  }
}
