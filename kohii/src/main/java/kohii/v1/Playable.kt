/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

import androidx.annotation.IntDef
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Playback.Callback
import kohii.v1.Playback.Priority
import kohii.v1.exo.ExoPlayable
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * One Playable to at most one Playback.
 *
 * Playable lifecycle:
 *
 * - Created by calling [Kohii], will be managed by at least one [PlaybackManager].
 * - Destroyed if:
 *  - All [PlaybackManager]s manage the Playable is destroyed/detached from its lifecycle.
 *
 * - A [Playable] can be bound to a Target to produce a [Playback]. Due to the reusable nature of
 * [Playable], the call to bind it is not limited to one Target. Which means that, a [Playable] can
 * be rebound to any number of other Targets. If a [Playable] X is bound to a Target A, and this binding
 * produced a [Playback] xRa, then the [Playable] X is rebound to a Target B, it will first produce
 * a produce a [Playback] xRb. But before that, the [Playback] xRa must also be destroyed.
 *
 * @author eneim (2018/06/24).
 */

/**
 * 2019/02/16
 *
 * A Playable should accept only one type of Target.
 */
interface Playable<T> : Callback {

  companion object {
    const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF
    const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE
    const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL

    const val STATE_IDLE = Player.STATE_IDLE
    const val STATE_BUFFERING = Player.STATE_BUFFERING
    const val STATE_READY = Player.STATE_READY
    const val STATE_END = Player.STATE_ENDED

    internal val NO_TAG = Any()
  }

  @Retention(SOURCE)
  @IntDef(REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL)
  annotation class RepeatMode

  @Retention(SOURCE)
  @IntDef(STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_END)
  annotation class State

  val tag: Any

  // fun bind(target: T, @Priority priority: Int) = bind(target, priority, cb = null)

  fun bind(
    provider: ContainerProvider,
    target: T
  ) = this.bind(
      provider,
      target, Playback.PRIORITY_NORMAL
  )

  fun bind(
    provider: ContainerProvider,
    target: T,
    @Priority priority: Int
  ): Playback<T>

  /// Playback controller

  // Must be called by Playback
  fun prepare()

  // Must be called by Playback
  fun play()

  // Must be called by Playback
  fun pause()

  // Must be called by Playback
  fun release()

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  // Getter
  val volumeInfo: VolumeInfo

  // Setter/Getter
  var playbackInfo: PlaybackInfo

  val delay: Long

  // internal API

  fun onPlaybackCreated(playback: Playback<T>)

  fun onPlaybackDestroyed(playback: Playback<T>)

  // data class for copying convenience.
  data class Builder(
    val kohii: Kohii,
    val media: Media,
    val playbackInfo: PlaybackInfo = PlaybackInfo.SCRAP,
    val tag: Any? = null,
    val delay: Long = 0,
    val prefetch: Boolean = false,
    @RepeatMode val repeatMode: Int = REPEAT_MODE_OFF, // FIXME 190104 should be Playback's option?
    val playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT
  ) {
    // Acquire Playable from cache or build new one. The result must not be mapped to any Manager.
    // If the builder has no valid tag (a.k.a tag is null), then always return new one.
    // TODO [20181021] Consider to make this to use the Factory mechanism?
    fun asPlayable(): Playable<PlayerView> {
      @Suppress("UNCHECKED_CAST")
      return ((
          if (tag != null)
            kohii.mapTagToPlayable.getOrPut(tag) { ExoPlayable(kohii, this) }
          else
            ExoPlayable(kohii, this)
          ) as Playable<PlayerView>).also {
        kohii.mapPlayableToManager[it] = null
      }
    }

    fun <T> bind(
      containerProvider: ContainerProvider,
      target: T, @Priority priority: Int
    ): Playback<T> {
      return if (target is PlayerView) {
        @Suppress("UNCHECKED_CAST")
        asPlayable().bind(containerProvider, target, priority) as Playback<T>
      } else throw IllegalArgumentException("Unsupported target: $target")
    }

    fun <T> bind(
      containerProvider: ContainerProvider,
      target: T
    ) = this.bind(containerProvider, target, Playback.PRIORITY_NORMAL)
  }
}