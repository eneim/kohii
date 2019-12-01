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

package kohii.v1.experiments.internal

import android.util.Log
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
import kohii.v1.core.VideoSize
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo
import kotlin.properties.Delegates

internal class UnofficialYouTubePlayerBridge(
  private val media: Media
) : AbstractBridge<YouTubePlayerView>() {

  internal fun mapState(original: PlayerState): Int {
    return when (original) {
      PLAYING -> Common.STATE_READY
      BUFFERING -> Common.STATE_BUFFERING
      ENDED -> Common.STATE_ENDED
      PAUSED -> Common.STATE_READY
      else -> Common.STATE_IDLE
    }
  }

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

  private val tracker = LocalPlayerTracker()

  private val playerListener = object : AbstractYouTubePlayerListener() {
    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState
    ) {
      tracker.state = state
      val kohiiState = mapState(state)
      Log.i(
          "Kohii::Art",
          "${tracker.videoId}, state: $state ($kohiiState), tracker state: ${tracker.state}"
      )
      eventListeners.onPlayerStateChanged(state == PLAYING, kohiiState)
    }

    override fun onError(
      youTubePlayer: YouTubePlayer,
      error: PlayerError
    ) {
      errorListeners.onError(RuntimeException(error.name))
    }
  }

  private var _playbackInfo: PlaybackInfo by Delegates.observable(
      PlaybackInfo(0, 0),
      onChange = { _, oldVal, newVal ->
        // Note: we ignore volume setting here.
        if (newVal.resumePosition != oldVal.resumePosition) {
          player?.seekTo(newVal.resumePosition.toFloat())
        }
      }
  )

  override var videoSize: VideoSize = VideoSize.ORIGINAL

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
      _playbackInfo = PlaybackInfo(0, tracker.currentSecond.toLong())
    }
  }

  // override var parameters: PlaybackParameters = PlaybackParameters.DEFAULT

  override var repeatMode: Int by Delegates.observable(
      Common.REPEAT_MODE_OFF,
      onChange = { _, _, _ -> /* youtube library doesn't have looping support */ })

  override val playerState: Int
    get() = mapState(tracker.state)

  override fun isPlaying(): Boolean {
    return tracker.state == PLAYING
  }

  override var volumeInfo: VolumeInfo = VolumeInfo()

  override fun seekTo(positionMs: Long) {
    val playbackInfo = this.playbackInfo
    playbackInfo.resumePosition = positionMs
    playbackInfo.resumeWindow = 0
    _playbackInfo = playbackInfo
    player?.seekTo(positionMs.toFloat())
  }

  override fun prepare(loadSource: Boolean) {
    // do nothing
  }

  override fun ready() {
    // do nothing
  }

  override fun play() {
    if (videoSize == VideoSize.NONE) return
    val videoId = media.uri.toString()
    if (tracker.state != PLAYING || tracker.videoId != videoId) {
      val player = this.player
      if (tracker.videoId == videoId && player != null) {
        player.play()
      } else {
        val startPos = _playbackInfo.resumePosition.toFloat()
        player?.loadVideo(videoId, startPos)
            ?: run {
              val playerView = requireNotNull(renderer)
              val callback = object : DelayedYouTubePlayerCallback(videoId, startPos) {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                  this@UnofficialYouTubePlayerBridge.player = youTubePlayer
                  super.onYouTubePlayer(youTubePlayer)
                }
              }
              playerView.getYouTubePlayerWhenReady(callback)
            }
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

  // Same as YouTubePlayerTracker, but fields are editable
  internal class LocalPlayerTracker : AbstractYouTubePlayerListener() {
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

  internal open class DelayedYouTubePlayerCallback(
    private val videoId: String,
    private val startPos: Float
  ) : YouTubePlayerCallback {
    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
      youTubePlayer.loadVideo(videoId, startPos)
    }
  }
}
