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

import android.graphics.Bitmap
import androidx.annotation.IntDef
import com.google.android.exoplayer2.Player
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import java.util.concurrent.Future
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * 2019/02/16
 *
 * A Playable should accept only one type of Target.
 *
 * @author eneim (2018/06/24).
 */
// TODO [20190430] Instead of defining output TYPE by parameter, consider to have "allows" method.
interface Playable<OUTPUT : Any> {

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

  val playbackState: Int

  val tag: Any

  val media: Media

  val isPlaying: Boolean

  val config: Config

  @RepeatMode
  var repeatMode: Int

  fun <CONTAINER : Any> bind(
    target: Target<CONTAINER, OUTPUT>,
    config: Playback.Config = Playback.Config(),
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  )

  // Playback controller

  fun prepare()

  fun ensurePreparation()

  fun play()

  fun pause()

  fun reset()

  fun release()

  fun seekTo(positionMs: Long)

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  // Getter
  val volumeInfo: VolumeInfo

  // Setter/Getter
  var playbackInfo: PlaybackInfo

  fun onPlayerActive(
    playback: Playback<OUTPUT>,
    player: OUTPUT
  )

  fun onPlayerInActive(
    playback: Playback<OUTPUT>,
    player: OUTPUT?
  )

  fun onAdded(playback: Playback<*>) {}

  fun onActive(playback: Playback<*>) {}

  fun onInActive(playback: Playback<*>) {}

  fun onRemoved(playback: Playback<*>) {}

  // data class for copying convenience.
  data class Config(
    val tag: String? = null,
    val preLoad: Boolean = false,
    val cover: Future<Bitmap?>? = null
  )
}
