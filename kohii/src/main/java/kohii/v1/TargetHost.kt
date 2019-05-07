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
import kohii.media.VolumeInfo
import kohii.v1.Playback.Companion.BOTH_AXIS_COMPARATOR
import kohii.v1.Playback.Companion.HORIZONTAL_COMPARATOR
import kohii.v1.Playback.Companion.VERTICAL_COMPARATOR
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A TargetHost is the representation of a View in Kohii. A TargetHost wraps a View and provides it
 * internal support such as listening to Scroll event, wrapping it with special LayoutParam to provide more control, etc.
 *
 * Implementation of a TargetHost must correctly override required methods, such as [select], [accepts], ...
 * The [PlaybackManager] will talk to TargetHost to ask for necessary information to update the overall behavior.
 */
abstract class TargetHost(
  val host: Any,
  val manager: PlaybackManager
) /* : Comparable<TargetHost> */ {

  companion object {
    internal const val VERTICAL = RecyclerView.VERTICAL
    internal const val HORIZONTAL = RecyclerView.HORIZONTAL
    internal const val BOTH_AXIS = -1
    internal const val NONE_AXIS = -2

    // read-only map
    val comparators = mapOf(
        HORIZONTAL to HORIZONTAL_COMPARATOR,
        VERTICAL to VERTICAL_COMPARATOR,
        BOTH_AXIS to BOTH_AXIS_COMPARATOR,
        NONE_AXIS to BOTH_AXIS_COMPARATOR
    )

    internal fun createTargetHost(
      host: Any,
      manager: PlaybackManager
    ): TargetHost? {
      return when (host) {
        is RecyclerView ->
          RecyclerViewTargetHost(host, manager)
        is NestedScrollView ->
          NestedScrollViewTargetHost(host, manager)
        is ViewPager ->
          ViewPagerTargetHost(host, manager)
        is ViewPager2 ->
          ViewPager2TargetHost(host, manager)
        is ViewGroup ->
          if (Build.VERSION.SDK_INT >= 23) ViewGroupTargetHostV23(host, manager)
          else ViewGroupTargetHostBase(host, manager)
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

  // state
  internal abstract val lock: AtomicBoolean

  internal abstract var volumeInfo: VolumeInfo

  internal open fun onAdded() {}

  internal open fun onRemoved() {}

  internal abstract fun <T> attachTarget(target: T)

  internal abstract fun <T> detachTarget(target: T)

  /**
   * Returns true if this TargetHost accepts a container. When a TargetHost accepts a container, it keeps track
   * of that container's state and send signal to the PlaybackManager when needed. A PlaybackManager has the
   * power to change a container's Host base on certain situation.
   */
  internal abstract fun accepts(container: Any): Boolean

  // Must contain and allow it to play.
  internal abstract fun allowsToPlay(playback: Playback<*>): Boolean

  internal open fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
  }

  data class Builder(
    val host: Any,
    val volumeInfo: VolumeInfo? = null
  ) {

    internal fun build(manager: PlaybackManager) = createTargetHost(this.host, manager)
  }
}
