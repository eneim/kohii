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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.ErrorReason
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle.MINIMAL
import com.google.android.youtube.player.YouTubePlayer.Provider
import kohii.v1.core.AbstractBridge
import kohii.v1.core.Common
import kohii.v1.experiments.YouTubePlayerFragment
import kohii.v1.experiments.performRelease
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo

internal class OfficialYouTubePlayerBridge(
  private val media: Media
) : AbstractBridge<YouTubePlayerFragment>(),
    PlaybackEventListener,
    PlayerStateChangeListener, DefaultLifecycleObserver {

  private val initializedListener = object : OnInitializedListener {
    override fun onInitializationSuccess(
      provider: Provider?,
      player: YouTubePlayer?,
      restored: Boolean
    ) {
      this@OfficialYouTubePlayerBridge.player = player
      // Start playback
      if (_playWhenReady && allowedToPlay()) {
        if (restored) {
          requireNotNull(player).play()
        } else {
          requireNotNull(player)
              .loadVideo(media.uri.toString(), _playbackInfo.resumePosition.toInt())
        }
      }
    }

    override fun onInitializationFailure(
      provider: Provider?,
      result: YouTubeInitializationResult?
    ) {
    }
  }

  internal var player: YouTubePlayer? = null
    set(value) {
      val from = field
      field = value
      val to = field
      if (from === to) return
      from?.performRelease()
      if (to != null) {
        to.setPlayerStateChangeListener(this)
        to.setPlaybackEventListener(this)
        to.setManageAudioFocus(true)
        to.setPlayerStyle(MINIMAL)
        to.setShowFullscreenButton(false)
      } else {
        _playWhenReady = false
        _playbackState = Common.STATE_IDLE
      }
    }

  private var _playbackState = Common.STATE_IDLE
    set(value) {
      val from = field
      field = value
      val to = field
      if (from == to) return
      this.eventListeners.onPlayerStateChanged(this._playWhenReady, to)
    }

  private var _playWhenReady: Boolean = false
  private var _loadedVideoId: String? = null

  private fun updatePlaybackInfo() {
    player?.let {
      if (_playbackState != Common.STATE_IDLE) {
        _playbackInfo = try {
          _playbackInfo.copy(resumePosition = it.currentTimeMillis.toLong())
        } catch (er: IllegalStateException) {
          PlaybackInfo()
        }
      }
    }
  }

  private fun allowedToPlay(): Boolean {
    return this.renderer?.allowedToPlay() == true
  }

  private var _playbackInfo: PlaybackInfo = PlaybackInfo()
    set(value) {
      val from = field
      field = value
      val to = field
      val posChanged = to.resumePosition != from.resumePosition
      if (posChanged) {
        val newPos = to.resumePosition.toInt()
        try {
          player?.seekToMillis(newPos)
        } catch (error: Exception) {
          error.printStackTrace()
        }
      }
    }

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo()
      return _playbackInfo
    }
    set(value) {
      _playbackInfo = value
    }

  override var volumeInfo: VolumeInfo = VolumeInfo.DEFAULT_ACTIVE

  override val playerState: Int
    get() = _playbackState

  override var renderer: YouTubePlayerFragment? = null
    set(value) {
      val from = field
      field = value
      val to = field
      if (from === to) return
      if (to != null && to.view != null) {
        to.removeLifecycleObserver(this@OfficialYouTubePlayerBridge)
      }
      if (from == null) {
        updatePlaybackInfo()
        player = null
      } else {
        from.addLifecycleObserver(this@OfficialYouTubePlayerBridge)
      }
    }

  override fun play() {
    super.play()
    if (!playerParameters.playerShouldStart()) return
    if (!this.isPlaying() || _loadedVideoId != media.uri.toString()) {
      this._playWhenReady = true
      this.renderer?.let {
        if (it.isVisible) {
          this.player?.play() ?: it.initialize(initializedListener)
        }
      }
    }
  }

  override fun pause() {
    super.pause()
    this._playWhenReady = false
    updatePlaybackInfo()
    try {
      player?.pause()
    } catch (er: Exception) {
      er.printStackTrace()
    }
    player = null
  }

  override fun isPlaying(): Boolean {
    return this.player != null && this._playWhenReady && this._playbackState > 1
  }

  override fun reset(resetPlayer: Boolean) {
    this.pause()
    _playWhenReady = false
    _playbackState = Common.STATE_IDLE
    _playbackInfo = PlaybackInfo()
  }

  override fun release() {
    this.reset()
    player = null
  }

  override fun seekTo(positionMs: Long) {
    val temp = this.playbackInfo
    _playbackInfo = PlaybackInfo(temp.resumeWindow, temp.resumePosition)
  }

  override var repeatMode = Common.REPEAT_MODE_OFF

  override fun prepare(loadSource: Boolean) {
    // no-ops
  }

  override fun ready() {
    // no-ops
  }

  // [BEGIN] PlaybackEventListener

  override fun onSeekTo(newPositionMillis: Int) {
    this.eventListeners.onSeekProcessed()
  }

  override fun onBuffering(isBuffering: Boolean) {
    Log.i("Kohii::YouTube", "Event: onBuffering $isBuffering")
    _playbackState = if (isBuffering) Common.STATE_BUFFERING else _playbackState
  }

  override fun onPlaying() {
    Log.i("Kohii::YouTube", "Event: onPlaying")
    _playbackState = Common.STATE_READY
  }

  override fun onPaused() {
    Log.i("Kohii::YouTube", "Event: onPaused")
    _playbackState = Common.STATE_READY
  }

  override fun onStopped() {
    Log.i("Kohii::YouTube", "Event: onStopped")
  }

  // [END] PlaybackEventListener

  // [BEGIN] PlayerStateChangeListener

  override fun onAdStarted() {
    Log.d("Kohii::YouTube", "State: onAdStarted")
  }

  override fun onLoading() {
    Log.d("Kohii::YouTube", "State: onLoading")
  }

  override fun onVideoStarted() {
    Log.d("Kohii::YouTube", "State: onVideoStarted")
    this.eventListeners.onRenderedFirstFrame()
  }

  override fun onLoaded(videoId: String?) {
    Log.d("Kohii::YouTube", "State: onLoaded $videoId")
    _loadedVideoId = videoId
  }

  override fun onVideoEnded() {
    Log.d("Kohii::YouTube", "State: onVideoEnded")
    _playbackState = Common.STATE_ENDED
  }

  override fun onError(reason: ErrorReason?) {
    Log.w("Kohii::YouTube", "State: onError $reason")
    _playbackState = Common.STATE_IDLE
    val error = RuntimeException(reason?.name ?: "Unknown error.")
    this.errorListeners.onError(error)
  }

  // [END] PlayerStateChangeListener

  // [BEGIN] LifecycleObserver

  override fun onPause(owner: LifecycleOwner) {
    updatePlaybackInfo()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    updatePlaybackInfo()
    player?.performRelease()
    _playWhenReady = false
    _playbackState = Common.STATE_IDLE
    owner.lifecycle.removeObserver(this)
  }
}
