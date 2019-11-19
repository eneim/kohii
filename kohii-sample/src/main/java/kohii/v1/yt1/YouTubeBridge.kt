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

package kohii.v1.yt1

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
import kohii.v1.core.Common
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo
import kohii.v1.core.AbstractBridge
import kotlin.properties.Delegates

class YouTubeBridge(
  private val media: Media
) : AbstractBridge<YouTubePlayerFragment>(),
    PlaybackEventListener,
    PlayerStateChangeListener, LifecycleObserver {

  private val initializedListener = object : OnInitializedListener {
    override fun onInitializationSuccess(
      provider: Provider?,
      player: YouTubePlayer?,
      restored: Boolean
    ) {
      this@YouTubeBridge.player = player
      // Start playback
      if (_playWhenReady && allowedToPlay()) {
        if (restored) {
          player?.play()
        } else {
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

  internal var player: YouTubePlayer? by Delegates.observable<YouTubePlayer?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        from?.performRelease()
        if (to != null) {
          to.setPlayerStateChangeListener(this)
          to.setPlaybackEventListener(this)
          to.setManageAudioFocus(true)
          to.setPlayerStyle(MINIMAL)
          to.setShowFullscreenButton(false)
        }
      }
  )

  private var _playbackState by Delegates.observable(
      initialValue = Common.STATE_IDLE,
      onChange = { _, oldVal, newVal ->
        if (oldVal == newVal) return@observable
        Log.i("Kohii::YT1", "state change: $newVal")
        // this.eventListeners.onPlayerStateChanged(this._playWhenReady, newVal)
      })

  private var _playWhenReady: Boolean = false
  private var _loadedVideoId: String? = null

  private fun updatePlaybackInfo() {
    player?.let {
      if (_playbackState != Common.STATE_IDLE) {
        _playbackInfo = _playbackInfo.copy(resumePosition = it.currentTimeMillis.toLong())
      }
    }
  }

  private fun allowedToPlay(): Boolean {
    return this.renderer?.allowedToPlay() == true
  }

  // Update this value will trigger player to seek.
  private var _playbackInfo: PlaybackInfo by Delegates.observable(
      initialValue = PlaybackInfo(),
      onChange = { _, oldVal, newVal ->
        val posChanged = newVal.resumePosition != oldVal.resumePosition
        if (posChanged) {
          val newPos = newVal.resumePosition.toInt()
          try {
            player?.seekToMillis(newPos)
          } catch (error: Exception) {
            error.printStackTrace()
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

  override val volumeInfo: VolumeInfo =
    VolumeInfo()

  override val playbackState: Int
    get() = _playbackState

  override var renderer: YouTubePlayerFragment? = null
    set(value) {
      if (field === value) return
      field?.also {
        if (it.view != null) it.viewLifecycleOwner.lifecycle.removeObserver(this)
      }
      if (value == null) {
        updatePlaybackInfo()
        player = null
      }
      value?.viewLifecycleOwner?.lifecycle?.addObserver(this)
      field = value
    }

  override fun play() {
    if (!this.isPlaying() || _loadedVideoId != media.uri.toString()) {
      this._playWhenReady = true
      this.renderer?.let {
        if (it.view != null) {
          this.player?.play() ?: it.initialize(initializedListener)
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
    player = null
  }

  override fun isPlaying(): Boolean {
    return this.player != null && this._playWhenReady
  }

  override var parameters: PlaybackParameters = PlaybackParameters.DEFAULT

  override fun reset(resetPlayer: Boolean) {
    this.pause()
    _playbackState = Common.STATE_IDLE
    _playbackInfo = PlaybackInfo()
  }

  override fun release() {
    this.reset()
    player = null
  }

  override fun seekTo(positionMs: Long) {
    val temp = this.playbackInfo
    _playbackInfo = PlaybackInfo(
        temp.resumeWindow, temp.resumePosition, this.volumeInfo
    )
  }

  override var repeatMode = Common.REPEAT_MODE_OFF

  override fun prepare(loadSource: Boolean) {
    // no-ops
  }

  override fun ready() {
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
    Log.i("Kohii::YT1", "Event: onBuffering $isBuffering")
    _playbackState = if (isBuffering) Common.STATE_BUFFERING else _playbackState
  }

  override fun onPlaying() {
    Log.i("Kohii::YT1", "Event: onPlaying")
    _playbackState = Common.STATE_READY
  }

  override fun onPaused() {
    Log.i("Kohii::YT1", "Event: onPaused")
    _playbackState = Common.STATE_READY
  }

  override fun onStopped() {
    Log.i("Kohii::YT1", "Event: onStopped")
    _playbackState = Common.STATE_ENDED
  }

  // [END] PlaybackEventListener

  // [BEGIN] PlayerStateChangeListener

  override fun onAdStarted() {
    Log.d("Kohii::YT1", "State: onAdStarted")
  }

  override fun onLoading() {
    Log.d("Kohii::YT1", "State: onLoading")
  }

  override fun onVideoStarted() {
    Log.d("Kohii::YT1", "State: onVideoStarted")
    this.eventListeners.onRenderedFirstFrame()
  }

  override fun onLoaded(videoId: String?) {
    Log.d("Kohii::YT1", "State: onLoaded $videoId")
    _loadedVideoId = videoId
  }

  override fun onVideoEnded() {
    Log.d("Kohii::YT1", "State: onVideoEnded")
    _playbackState = Common.STATE_ENDED
  }

  override fun onError(reason: ErrorReason?) {
    Log.w("Kohii::YT1", "State: onError $reason")
    val error = RuntimeException(reason?.name ?: "Unknown error.")
    this.errorListeners.onError(error)
  }

  // [END] PlayerStateChangeListener

  // [BEGIN] LifecycleObserver

  @OnLifecycleEvent(ON_PAUSE)
  internal fun onOwnerPause() {
    updatePlaybackInfo()
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onOwnerDestroy(owner: LifecycleOwner) {
    updatePlaybackInfo()
    player?.performRelease()
    _playbackState = Common.STATE_IDLE
    owner.lifecycle.removeObserver(this)
  }
}

internal fun YouTubePlayer.performRelease() {
  try {
    setPlayerStateChangeListener(null)
    setPlaybackEventListener(null)
    setPlaylistEventListener(null)
    setOnFullscreenListener(null)
    setManageAudioFocus(false)
    setShowFullscreenButton(false)
    release()
  } catch (error: Exception) {
    error.printStackTrace()
  }
}
