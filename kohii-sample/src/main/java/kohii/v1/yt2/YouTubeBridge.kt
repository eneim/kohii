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

package kohii.v1.yt2

import com.google.android.exoplayer2.PlaybackParameters
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.BUFFERING
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.ENDED
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PAUSED
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.UNKNOWN
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.core.AbstractBridge
import kohii.v1.core.Common
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo
import kotlin.properties.Delegates

class YouTubeBridge(
  private val media: Media
) : AbstractBridge<YouTubePlayerView>() {

  private var player: YouTubePlayer? by Delegates.observable<YouTubePlayer?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        updatePlaybackInfo(from)
        tracker.videoId = null
        tracker.state = UNKNOWN
        if (from != null) {
          from.removeListener(tracker)
          from.removeListener(playerListener)
        }
        if (to != null) {
          to.addListener(tracker)
          to.addListener(playerListener)
        }
      }
  )

  fun mapState(original: PlayerState): Int {
    return when (original) {
      PLAYING -> Common.STATE_READY
      BUFFERING -> Common.STATE_BUFFERING
      ENDED -> Common.STATE_ENDED
      PAUSED -> Common.STATE_READY
      else -> Common.STATE_IDLE
    }
  }

  private val tracker = PlayerTracker()
  private val playerListener = object : AbstractYouTubePlayerListener() {
    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState
    ) {
      eventListeners.onPlayerStateChanged(state === PLAYING, mapState(state))
    }

    override fun onError(
      youTubePlayer: YouTubePlayer,
      error: PlayerError
    ) {
      errorListeners.onError(RuntimeException(error.name))
    }
  }

  private var _playbackInfo: PlaybackInfo by Delegates.observable(
      PlaybackInfo(
          0, 0, VolumeInfo()
      ),
      onChange = { _, _, _ ->
        // Note: we ignore volume setting here.
        // if (newVal.resumePosition != oldVal.resumePosition) {
        //   player?.seekTo(newVal.resumePosition.toFloat())
        // }
      }
  )

  override var renderer: YouTubePlayerView? = null
    set(value) {
      if (field === value) return
      if (value == null) player = null
      field = value
    }

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo(player)
      return _playbackInfo
    }
    set(value) {
      _playbackInfo = value
      player?.seekTo(value.resumePosition.toFloat())
    }

  private fun updatePlaybackInfo(player: YouTubePlayer?) {
    if (player != null) {
      _playbackInfo = PlaybackInfo(
          0, tracker.currentSecond.toLong(), _playbackInfo.volumeInfo
      )
    }
  }

  override var parameters: PlaybackParameters = PlaybackParameters.DEFAULT

  override var repeatMode: Int by Delegates.observable(
      Common.REPEAT_MODE_OFF,
      onChange = { _, _, _ -> /* youtube library doesn't have looping support */ })

  override val playbackState: Int
    get() = mapState(tracker.state)

  override fun isPlaying(): Boolean {
    return tracker.state === PLAYING
  }

  override val volumeInfo: VolumeInfo =
    VolumeInfo()

  override fun seekTo(positionMs: Long) {
    val playbackInfo = this.playbackInfo
    playbackInfo.resumePosition = positionMs
    playbackInfo.resumeWindow = 0
    _playbackInfo = playbackInfo
    player?.seekTo(positionMs.toFloat())
  }

  override fun prepare(loadSource: Boolean) {
    // no-ops
  }

  override fun ready() {
    // no-ops
  }

  private val startCallback = object : YouTubePlayerCallback {
    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
      player = youTubePlayer
      youTubePlayer.loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toFloat())
    }
  }

  override fun play() {
    if (tracker.state !== PLAYING || tracker.videoId != media.uri.toString()) {
      val player = this.player
      val playerView = requireNotNull(renderer)
      if (tracker.videoId == media.uri.toString() && player != null) {
        player.play()
      } else {
        player?.loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toFloat())
            ?: playerView.getYouTubePlayerWhenReady(startCallback)
      }
    }
  }

  override fun pause() {
    updatePlaybackInfo(player)
    player?.pause()
  }

  override fun reset(resetPlayer: Boolean) {
    _playbackInfo = PlaybackInfo()
    player?.pause()
  }

  override fun release() {
    updatePlaybackInfo(player)
    player?.also {
      it.removeListener(tracker)
      it.removeListener(playerListener)
    }
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return false
  }

  // Same as YouTubePlayerTracker, but fields are editable
  internal class PlayerTracker : AbstractYouTubePlayerListener() {
    /**
     * @return the player state. A value from [PlayerConstants.PlayerState]
     */
    var state: PlayerState = UNKNOWN
    var currentSecond: Float = 0f
    var videoDuration: Float = 0f
    var videoId: String? = null

    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState
    ) {
      this.state = state
    }

    override fun onCurrentSecond(
      youTubePlayer: YouTubePlayer,
      second: Float
    ) {
      currentSecond = second
    }

    override fun onVideoDuration(
      youTubePlayer: YouTubePlayer,
      duration: Float
    ) {
      videoDuration = duration
    }

    override fun onVideoId(
      youTubePlayer: YouTubePlayer,
      videoId: String
    ) {
      this.videoId = videoId
    }
  }
}
