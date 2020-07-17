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

package kohii.v1.x

import androidx.media2.common.MediaItem
import androidx.media2.common.SessionPlayer
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.widget.VideoView
import kohii.v1.core.AbstractBridge
import kohii.v1.core.PlayerParameters
import kohii.v1.core.PlayerPool
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo

/**
 * [kohii.v1.core.Bridge] for [VideoView]
 */
// Really experimental implementation of AbstractBridge using androidx.media2 APIs.
// Not production-ready.
class VideoViewBridge(
  private val media: Media,
  private val playerPool: PlayerPool<MediaPlayer>
) : AbstractBridge<VideoView>() {

  private val mediaItem: MediaItem = UriMediaItem.Builder(media.uri)
      .build()

  private var player: MediaPlayer? = null

  override var renderer: VideoView? = null
    set(value) {
      if (field === value) return // same reference
      field = value
      val player = this.player
      if (player != null && value != null) value.setPlayer(player)
    }

  override val playerState: Int
    get() = this.player?.playerState ?: SessionPlayer.PLAYER_STATE_IDLE

  override fun isPlaying(): Boolean =
    this.player?.playerState == SessionPlayer.PLAYER_STATE_PLAYING

  override fun seekTo(positionMs: Long) {
    this.player?.seekTo(positionMs)
  }

  override var repeatMode: Int = player?.repeatMode ?: SessionPlayer.REPEAT_MODE_NONE
    set(value) {
      field = value
      player?.repeatMode = value
    }

  override var playbackInfo: PlaybackInfo = PlaybackInfo()

  override var volumeInfo: VolumeInfo = VolumeInfo()

  // TODO(eneim): update the Player with new parameters.
  override var playerParameters: PlayerParameters = PlayerParameters.DEFAULT

  override fun prepare(loadSource: Boolean) {
    if (loadSource) ready()
  }

  override fun ready() {
    if (player == null) {
      player = playerPool.getPlayer(media)
          .also {
            it.setMediaItem(mediaItem)
            it.repeatMode = repeatMode
            renderer?.setPlayer(it)
            it.prepare()
          }
    }
  }

  override fun play() {
    super.play()
    requireNotNull(player).play()
  }

  override fun pause() {
    super.pause()
    player?.pause()
  }

  override fun reset(resetPlayer: Boolean) {
    player?.reset()
  }

  override fun release() {
    player?.let {
      // TODO any other local clean up?
      it.reset()
      playerPool.putPlayer(media, it)
    }
    player = null
  }
}
