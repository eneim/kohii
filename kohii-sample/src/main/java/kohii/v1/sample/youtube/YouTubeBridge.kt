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

package kohii.v1.sample.youtube

import android.util.Log
import com.google.android.exoplayer2.PlaybackParameters
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.UNKNOWN
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Bridge
import kohii.v1.ErrorListener
import kohii.v1.ErrorListeners
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.PlayerEventListeners
import kohii.v1.VolumeChangedListener
import kohii.v1.VolumeChangedListeners

class YouTubeBridge(
  private val media: Media
) : Bridge<YouTubePlayerView> {

  private val eventListeners by lazy { PlayerEventListeners() } // Set, so no duplicated
  private val volumeListeners by lazy { VolumeChangedListeners() } // Set, so no duplicated
  private val errorListeners by lazy { ErrorListeners() } // Set, so no duplicated

  private var player: YouTubePlayer? = null

  private val tracker = PlayerTracker()
  private val playerListener = object : AbstractYouTubePlayerListener() {
    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerState
    ) {
      Log.w("Kohii::YT", "state change to $state, media: ${media.uri}, tracker: ${tracker.videoId}")
      eventListeners.onPlayerStateChanged(state == PLAYING, state.ordinal)
    }

    override fun onVideoId(
      youTubePlayer: YouTubePlayer,
      videoId: String
    ) {
      Log.w("Kohii::YT", "video change: $videoId")
    }

    override fun onError(
      youTubePlayer: YouTubePlayer,
      error: PlayerError
    ) {
      Log.e("Kohii::YT", "error: $error")
      errorListeners.onError(RuntimeException(error.name))
    }
  }

  private var _playbackInfo =
    PlaybackInfo(0, 0, VolumeInfo()) // Backing field for PlaybackInfo set/get

  override var playerView: YouTubePlayerView? = null
    set(value) {
      if (field === value) return
      if (value == null) {
        updatePlayer(null)
      }
      field = value
    }

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo()
      return _playbackInfo
    }
    set(value) {
      _playbackInfo = value
      this.seekTo(_playbackInfo.resumePosition)
    }

  private fun updatePlaybackInfo() {
    player?.also {
      if (tracker.state === UNKNOWN) return
      _playbackInfo.resumeWindow = 0
      _playbackInfo.resumePosition = tracker.currentSecond.toLong()
    }
  }

  override var parameters: PlaybackParameters = PlaybackParameters.DEFAULT

  override var repeatMode: Int = Playable.REPEAT_MODE_ONE

  override val isPlaying: Boolean = tracker.state === PLAYING

  override val volumeInfo: VolumeInfo = VolumeInfo()

  override fun seekTo(positionMs: Long) {
    val playbackInfo = this.playbackInfo
    playbackInfo.resumePosition = positionMs
    playbackInfo.resumeWindow = 0
    this.playbackInfo = playbackInfo
  }

  override fun prepare(loadSource: Boolean) {
    // no-ops
  }

  override fun ensureResource() {
    // no-ops
  }

  private val startCallback = object : YouTubePlayerCallback {
    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
      updatePlayer(youTubePlayer)
      youTubePlayer.loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toFloat())
    }
  }

  private fun updatePlayer(youTubePlayer: YouTubePlayer?) {
    if (this.player === youTubePlayer) return
    updatePlaybackInfo()
    tracker.videoId = null
    tracker.state = UNKNOWN
    this.player?.also {
      it.removeListener(tracker)
      it.removeListener(playerListener)
    }
    this.player = youTubePlayer?.also {
      it.addListener(tracker)
      it.addListener(playerListener)
    }
  }

  override fun play() {
    if (tracker.state !== PLAYING || tracker.videoId != media.uri.toString()) {
      val player = this.player
      if (tracker.videoId === media.uri.toString() && player != null) {
        player.play()
      } else {
        player?.loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toFloat())
            ?: playerView?.getYouTubePlayerWhenReady(startCallback)
      }
    }
  }

  override fun pause() {
    updatePlaybackInfo()
    player?.pause()
  }

  override fun reset() {
    this.playbackInfo = PlaybackInfo()
    player?.pause()
  }

  override fun release() {
    updatePlaybackInfo()
    player?.also {
      it.removeListener(tracker)
      it.removeListener(playerListener)
    }
    player = null
    playerView = null
  }

  override fun addEventListener(listener: PlayerEventListener) {
    this.eventListeners.add(listener)
  }

  override fun removeEventListener(listener: PlayerEventListener?) {
    this.eventListeners.remove(listener)
  }

  override fun addVolumeChangeListener(listener: VolumeChangedListener) {
    this.volumeListeners.add(listener)
  }

  override fun removeVolumeChangeListener(listener: VolumeChangedListener?) {
    this.volumeListeners.remove(listener)
  }

  override fun addErrorListener(errorListener: ErrorListener) {
    this.errorListeners.add(errorListener)
  }

  override fun removeErrorListener(errorListener: ErrorListener?) {
    this.errorListeners.remove(errorListener)
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return false
  }

  // Same as YouTubePlayerTracker, but fields are editable
  class PlayerTracker : AbstractYouTubePlayerListener() {
    /**
     * @return the player state. A value from [PlayerConstants.PlayerState]
     */
    var state: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNKNOWN
    var currentSecond: Float = 0f
    var videoDuration: Float = 0f
    var videoId: String? = null

    override fun onStateChange(
      youTubePlayer: YouTubePlayer,
      state: PlayerConstants.PlayerState
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
