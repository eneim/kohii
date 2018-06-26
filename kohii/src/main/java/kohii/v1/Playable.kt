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

import android.net.Uri
import android.support.annotation.IntDef
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.v1.exo.Config
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * @author eneim (2018/06/24).
 */
interface Playable {

  companion object {
    const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF
    const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE
    const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL
  }

  @Retention(SOURCE)
  @IntDef(REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL)
  annotation class RepeatMode

  fun bind(playerView: PlayerView): Playback<PlayerView>

  /// Playback controller

  fun play()

  fun pause()

  fun release()

  fun addVolumeChangeListener(listener: OnVolumeChangedListener)

  fun removeVolumeChangeListener(listener: OnVolumeChangedListener)

  fun setPlaybackInfo(playbackInfo: PlaybackInfo)

  fun getPlaybackInfo(): PlaybackInfo

  // TODO [20180622] Should be hidden to User. Consider to make Playable abstract class
  fun mayUpdateStatus(manager: Manager, active: Boolean)

  data class Options(
      val kohii: Kohii,
      val uri: Uri,
      val config: Config = Config.DEFAULT_CONFIG,
      val playbackInfo: PlaybackInfo = PlaybackInfo.SCRAP,
      val mediaType: String? = null,
      val tag: Any? = null,
      val prepareAlwaysLoad: Boolean = false,
      @RepeatMode val repeatMode: Int = REPEAT_MODE_OFF
  ) {
    fun asPlayable(): Playable {
      return this.kohii.getPlayable(Bundle(this.uri, this))
    }
  }

  data class Bundle(val uri: Uri, val options: Options)
}