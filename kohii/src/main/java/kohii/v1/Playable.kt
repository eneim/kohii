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
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Playback.Callback
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * 2019/02/16
 *
 * A Playable should accept only one type of Target.
 *
 * @author eneim (2018/06/24).
 */
interface Playable<PLAYER> : Callback {

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

  fun <TARGET: Any> bind(
    target: TARGET,
    config: Playback.Config = Playback.Config(),
    cb: ((Playback<TARGET, PLAYER>) -> Unit)? = null
  )

  /// Playback controller

  fun prepare()

  fun play()

  fun pause()

  fun release()

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  // Getter
  val volumeInfo: VolumeInfo

  // Setter/Getter
  var playbackInfo: PlaybackInfo

  // data class for copying convenience.
  data class Config(
    val tag: String? = null,
    val playbackInfo: PlaybackInfo = PlaybackInfo.SCRAP,
    val startDelay: Int = 0, // Delay on every "play" call.
    val prefetch: Boolean = false,
    @RepeatMode val repeatMode: Int = REPEAT_MODE_OFF,
    val playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT
  ) {

    fun copySelf(): Config {
      return Config(
          tag = this.tag,
          playbackInfo = this.playbackInfo,
          prefetch = this.prefetch,
          repeatMode = this.repeatMode,
          playbackParameters = this.playbackParameters,
          startDelay = this.startDelay
      )
    }
  }
}