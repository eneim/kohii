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

package kohii.v1.exo

import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.PlaybackInfo.Companion.INDEX_UNSET
import kohii.media.PlaybackInfo.Companion.TIME_UNSET
import kohii.media.VolumeInfo
import kohii.v1.Bridge
import kohii.v1.DefaultEventListener
import kohii.v1.ErrorListener
import kohii.v1.ErrorListeners
import kohii.v1.Kohii
import kohii.v1.Playable.Builder
import kohii.v1.PlayerEventListener
import kohii.v1.PlayerEventListeners
import kohii.v1.VolumeChangedListener
import kohii.v1.VolumeChangedListeners
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
class ExoBridge(
    private val kohii: Kohii,
    private val builder: Builder
) : DefaultEventListener(), Bridge {

  private val eventListeners by lazy { PlayerEventListeners() }  // original listener.
  private val volumeListeners by lazy { VolumeChangedListeners() }
  private val errorListeners by lazy { ErrorListeners() }

  private val listenerIsSet = AtomicBoolean(false)

  private val _playbackInfo = PlaybackInfo()
  private var mediaSource: MediaSource? = null
  private var player: Player? = null

  override fun prepare(loadSource: Boolean) {
    if (loadSource) {
      ensureMediaSource()
      ensurePlayerView()
    }
  }

  override var playerView: PlayerView? = null
    set(value) {
      if (field === value) return
      if (value == null)
        field!!.player = null
      else {
        this.player?.run {
          PlayerView.switchTargetView(this, field, value)
        }
      }

      field = value
    }

  override fun play() {
    ensureMediaSource()
    ensurePlayerView()
    player!!.playWhenReady = true
  }

  override fun pause() {
    player?.playWhenReady = false
  }

  override fun reset() {
    playbackInfo.reset()
    player?.stop(true)
    // TODO [20180214] double check this when ExoPlayer 2.7.0 is released.
    // TODO [20180326] reusable MediaSource will be added after ExoPlayer 2.7.1.
    // TODO [20180626] double check 2.8.x
    this.mediaSource = null // so it will be re-prepared when play() is called.
  }

  override fun release() {
    this.playerView = null
    if (player != null) {
      player!!.stop(true)
      if (listenerIsSet.compareAndSet(true, false)) {
        player!!.removeListener(eventListeners)
        if (player is KohiiPlayer) {
          (player as KohiiPlayer).clearOnVolumeChangedListener()
        }

        if (player is SimpleExoPlayer) {
          (player as SimpleExoPlayer).apply {
            this.removeTextOutput(eventListeners)
            this.removeVideoListener(eventListeners)
            this.removeMetadataOutput(eventListeners)
          }
        }
        player!!.removeListener(this)
      }

      kohii.store.releasePlayer(player!!, builder.config)
    }

    this.player = null
    this.mediaSource = null
  }

  override fun addEventListener(listener: PlayerEventListener) {
    this.eventListeners.add(listener)
  }

  override fun removeEventListener(listener: PlayerEventListener?) {
    this.eventListeners.remove(listener)
  }

  override fun addVolumeChangeListener(listener: VolumeChangedListener) {
    this.volumeListeners.add(listener)
    (player as? KohiiPlayer)?.addOnVolumeChangedListener(listener)
  }

  override fun removeVolumeChangeListener(listener: VolumeChangedListener?) {
    this.volumeListeners.remove(listener)
    (player as? KohiiPlayer)?.removeOnVolumeChangedListener(listener)
  }

  override fun addErrorListener(errorListener: ErrorListener) {
    this.errorListeners.add(errorListener)
  }

  override fun removeErrorListener(errorListener: ErrorListener?) {
    this.errorListeners.remove(errorListener)
  }

  override val isPlaying: Boolean
    get() = player?.playWhenReady ?: false

  override val volumeInfo: VolumeInfo
    get() = playbackInfo.volumeInfo

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val changed = playbackInfo.volumeInfo != volumeInfo
    if (changed) {
      playbackInfo.volumeInfo = volumeInfo
      ExoStore.setVolumeInfo(player!!, playbackInfo.volumeInfo)
    }
    return changed
  }

  override var parameters: PlaybackParameters?
    get() = player!!.playbackParameters
    set(value) {
      player!!.playbackParameters = value
    }

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo()
      return _playbackInfo
    }
    set(value) {
      _playbackInfo.resumeWindow = value.resumeWindow
      _playbackInfo.resumePosition = value.resumePosition
      _playbackInfo.volumeInfo = value.volumeInfo

      if (player != null) {
        ExoStore.setVolumeInfo(player!!, _playbackInfo.volumeInfo)
        val haveResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
        if (haveResumePosition) {
          player!!.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
        }
      }
    }

  override fun onPlayerError(error: ExoPlaybackException?) {
    super.onPlayerError(error)
    if (error != null) this.errorListeners.onError(error)
  }

  private fun updatePlaybackInfo() {
    if (player == null) return
    if (player!!.playbackState == Player.STATE_IDLE) return
    _playbackInfo.resumeWindow = player?.currentWindowIndex ?: INDEX_UNSET
    _playbackInfo.resumePosition = if (player?.isCurrentWindowSeekable == true)
      Math.max(0, player?.currentPosition ?: 0)
    else
      TIME_UNSET
    _playbackInfo.volumeInfo = VolumeInfo.SCRAP
  }

  private fun ensurePlayerView() {
    playerView?.run {
      if (this.player != this@ExoBridge.player) this.player = this@ExoBridge.player
    }
  }

  private fun ensureMediaSource() {
    if (mediaSource == null) {  // Only actually prepare the source on demand.
      ensurePlayer()
      mediaSource = kohii.store.createMediaSource(this.builder)
      (player as? KohiiPlayer)?.prepare(mediaSource,
          playbackInfo.resumeWindow == INDEX_UNSET, false)
    }
  }

  private fun ensurePlayer() {
    Log.d("Bridge:" + hashCode(), "null() called")
    // 1. If player is set to null somewhere
    if (player == null) {
      player = kohii.store.acquirePlayer(this.builder.config)
      player!!.repeatMode = builder.repeatMode
      ExoStore.setVolumeInfo(player!!, _playbackInfo.volumeInfo)
      val haveResumePosition = _playbackInfo.resumeWindow != PlaybackInfo.INDEX_UNSET
      if (haveResumePosition) {
        player!!.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
      }
      listenerIsSet.set(false)
    }

    // 2. Player is just created, or Player is not null, but the listeners are reset.
    if (listenerIsSet.compareAndSet(false, true)) {
      player!!.addListener(this)
      player!!.addListener(eventListeners)
      (player as? SimpleExoPlayer)?.run {
        this.addVideoListener(eventListeners)
        this.addTextOutput(eventListeners)
        this.addMetadataOutput(eventListeners)
      }

      (player as? KohiiPlayer)?.run {
        this.addOnVolumeChangedListener(volumeListeners)
      }
    }
  }
}