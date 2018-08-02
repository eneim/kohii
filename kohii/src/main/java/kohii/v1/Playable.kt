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
import androidx.annotation.IntDef
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.v1.exo.Config
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * One Playable to at most one Playback.
 *
 * Playable lifecycle:
 *
 * - Created by calling [Kohii.setUp], will be managed by at least one [Manager].
 * - Destroyed if:
 *  - All [Manager] manage the Playable is destroyed/detached from its lifecycle.
 *
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

  fun prepare()

  fun play()

  fun pause()

  fun release()

  var playbackInfo: PlaybackInfo

  // data class for copying convenience.
  data class Builder(
      val kohii: Kohii,
      val contentUri: Uri,
      val config: Config = Config.DEFAULT_CONFIG,
      val playbackInfo: PlaybackInfo = PlaybackInfo.SCRAP,
      val mediaType: String? = null,
      val tag: Any? = null,
      val prepareAlwaysLoad: Boolean = false,
      @RepeatMode val repeatMode: Int = REPEAT_MODE_OFF
  ) {
    fun asPlayable(): Playable {
      return this.kohii.acquirePlayable(this.contentUri, this)
    }

    // for Java's Builder usage.

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(tag = tag)", "kohii.v1.Playable.Builder"))
    fun setTag(tag: Any?) = this.copy(tag = tag)

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(config = config)", "kohii.v1.Playable.Builder"))
    fun setConfig(config: Config) = this.copy(config = config)

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(mediaType = mediaType)", "kohii.v1.Playable.Builder"))
    fun setMediaType(mediaType: String?) = this.copy(mediaType = mediaType)

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(playbackInfo = playbackInfo)", "kohii.v1.Playable.Builder"))
    fun setPlaybackInfo(playbackInfo: PlaybackInfo) = this.copy(playbackInfo = playbackInfo)

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(repeatMode = repeatMode)", "kohii.v1.Playable.Builder"))
    fun setRepeatMode(repeatMode: Int) = this.copy(repeatMode = repeatMode)

    @Deprecated("Kohii works better with Kotlin than Java.",
        ReplaceWith("this.copy(prepareAlwaysLoad = prepareAlwaysLoad)",
            "kohii.v1.Playable.Builder"))
    fun setPrepareAlwaysLoad(prepareAlwaysLoad: Boolean) = this.copy(
        prepareAlwaysLoad = prepareAlwaysLoad)
  }
}