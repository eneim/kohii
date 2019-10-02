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

package kohii.v1.ytb

import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.ErrorReason
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle.MINIMAL
import com.google.android.youtube.player.YouTubePlayer.Provider
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.BaseBridge
import kohii.v1.Playable
import kotlin.properties.Delegates

class YouTubeBridge(
  private val media: Media
) : BaseBridge<YouTubePlayerFragment>(),
    PlaybackEventListener,
    PlayerStateChangeListener,
    LifecycleObserver {

  private val initializedListener = object : OnInitializedListener {
    override fun onInitializationSuccess(
      provider: Provider?,
      player: YouTubePlayer?,
      restored: Boolean
    ) {
      updatePlayer(player)
      // Start playback
      if (_playWhenReady && allowedToPlay()) {
        if (restored) player?.play()
        else {
          player?.loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toInt())
        }
      }
    }

    override fun onInitializationFailure(
      provider: Provider?,
      result: YouTubeInitializationResult?
    ) {
    }
  }

  var player: YouTubePlayer? = null

  private var _playbackState by Delegates.observable(Playable.STATE_IDLE,
      onChange = { _, _, newVal ->
        Log.i("Kohii::YTT", "state change: $newVal")
      })

  private var _playWhenReady: Boolean = false
  private var _loadedVideoId: String? = null

  private fun updatePlayer(youTubePlayer: YouTubePlayer?) {
    if (this.player === youTubePlayer) return
    this.player?.also {
      try {
        releasePlayer()
        Log.d("Kohii::YT", "updatePlayer($youTubePlayer) done")
      } catch (error: Exception) {
        Log.w("Kohii::YT", "updatePlayer($youTubePlayer) throws error: $error")
      }
    }
    this.player = youTubePlayer?.also {
      it.setPlayerStateChangeListener(this)
      it.setPlaybackEventListener(this)
      it.setManageAudioFocus(true)
      it.setPlayerStyle(MINIMAL)
      it.setShowFullscreenButton(false)
    }
  }

  internal fun updatePlaybackInfo() {
    player?.let {
      if (_playbackState != Playable.STATE_IDLE) {
        _playbackInfo = _playbackInfo.copy(resumePosition = it.currentTimeMillis.toLong())
      }
    }
  }

  private fun releasePlayer() {
    Log.d("Kohii::YTT", "release $player")
    this.player?.let {
      it.setPlayerStateChangeListener(null)
      it.setPlaybackEventListener(null)
      it.setPlaylistEventListener(null)
      it.setOnFullscreenListener(null)
      it.setManageAudioFocus(false)
      it.setShowFullscreenButton(false)
      it.release()
    }
  }

  private fun allowedToPlay(): Boolean {
    return this.playerView?.allowedToPlay() == true
  }

  // Update this value will trigger player to seek.
  private var _playbackInfo: PlaybackInfo by Delegates.observable(PlaybackInfo(),
      onChange = { _, oldVal, newVal ->
        Log.i("Kohii::YTT", "info changed: $newVal")
        val newPos = newVal.resumePosition.toInt()
        val posChanged = newVal.resumePosition != oldVal.resumePosition
        if (posChanged) {
          try {
            player?.seekToMillis(newPos)
          } catch (error: Exception) {
            Log.w("Kohii::YT", "seekToMs($newPos) throws error: $error")
          }
        }
      })

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo()
      return _playbackInfo
    }
    set(value) {
      _playbackInfo = value
    }

  override val volumeInfo: VolumeInfo = VolumeInfo()

  override val playbackState: Int
    get() = _playbackState

  override var playerView: YouTubePlayerFragment? = null
    set(value) {
      if (field === value) return
      field?.also {
        if (it.view != null) it.viewLifecycleOwner.lifecycle.removeObserver(this)
      }
      if (value == null) {
        updatePlayer(null)
      }
      field = value
      field?.also {
        it.viewLifecycleOwner.lifecycle.addObserver(this)
      }
    }

  override fun play() {
    Log.d("Kohii::YT", "play: ${this.playerView}")
    if (!this.isPlaying() || _loadedVideoId != media.uri.toString()) {
      this._playWhenReady = true
      this.playerView?.let {
        if (it.view != null) {
          @Suppress("CAST_NEVER_SUCCEEDS")
          this.player?.play() ?: it.initialize(initializedListener)
        } else {
          Log.e("Kohii::YT", "player invisible: ${this.playerView}")
        }
      }
    }
  }

  override fun pause() {
    this._playWhenReady = false
    updatePlaybackInfo()
    try {
      player?.pause()
    } catch (er: DeadObjectException) {
      er.printStackTrace()
    }
    releasePlayer()
    player = null
  }

  override fun isPlaying(): Boolean {
    return this.player != null && this._playWhenReady
  }

  override var parameters: PlaybackParameters = PlaybackParameters.DEFAULT

  override fun reset() {
    this.pause()
    _playbackState = Playable.STATE_IDLE
    _playbackInfo = PlaybackInfo()
  }

  override fun release() {
    this.reset()
    releasePlayer()
    player = null
  }

  override fun seekTo(positionMs: Long) {
    val temp = this.playbackInfo
    _playbackInfo = PlaybackInfo(temp.resumeWindow, temp.resumePosition, this.volumeInfo)
  }

  override var repeatMode = Playable.REPEAT_MODE_OFF

  override fun prepare(loadSource: Boolean) {
    // no-ops
  }

  override fun ensurePreparation() {
    // no-ops
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return false
  }

  // [BEGIN] PlaybackEventListener

  override fun onSeekTo(newPositionMillis: Int) {
    this.eventListeners.onSeekProcessed()
  }

  override fun onBuffering(isBuffering: Boolean) {
    _playbackState = if (isBuffering) Playable.STATE_BUFFERING else _playbackState
    // this.eventListeners.onPlayerStateChanged(this._playWhenReady, this._playbackState)
  }

  override fun onPlaying() {
    _playbackState = Playable.STATE_READY
    // this.eventListeners.onPlayerStateChanged(this._playWhenReady, this._playbackState)
  }

  override fun onStopped() {
    _playbackState = Playable.STATE_END
    // this.eventListeners.onPlayerStateChanged(this._playWhenReady, this._playbackState)
  }

  override fun onPaused() {
    _playbackState = Playable.STATE_READY
    // this.eventListeners.onPlayerStateChanged(this._playWhenReady, this._playbackState)
  }

  // [END] PlaybackEventListener

  // [BEGIN] PlayerStateChangeListener

  override fun onAdStarted() {
  }

  override fun onLoading() {
  }

  override fun onVideoStarted() {
    this.eventListeners.onRenderedFirstFrame()
  }

  override fun onLoaded(videoId: String?) {
    _loadedVideoId = videoId
  }

  override fun onVideoEnded() {
    _playbackState = Playable.STATE_END
    // this.eventListeners.onPlayerStateChanged(this._playWhenReady, this._playbackState)
  }

  override fun onError(reason: ErrorReason?) {
    val error = RuntimeException(reason?.name ?: "Unknown")
    this.errorListeners.onError(error)
  }

  // [END] PlayerStateChangeListener

  // [BEGIN] LifecycleObserver

  @OnLifecycleEvent(ON_PAUSE)
  fun onOwnerPause() {
    updatePlaybackInfo()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    releasePlayer()
    owner.lifecycle.removeObserver(this)
  }
}
