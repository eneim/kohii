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

import android.util.Pair
import android.widget.Toast
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kohii.addEventListener
import kohii.getVolumeInfo
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.PlaybackInfo.Companion.INDEX_UNSET
import kohii.media.PlaybackInfo.Companion.TIME_UNSET
import kohii.media.VolumeInfo
import kohii.removeEventListener
import kohii.setVolumeInfo
import kohii.v1.Bridge
import kohii.v1.ErrorListener
import kohii.v1.ErrorListeners
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.PlayerEventListeners
import kohii.v1.R
import kohii.v1.VolumeChangedListener
import kohii.v1.VolumeChangedListeners
import kohii.v1.VolumeInfoController
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
internal open class ExoPlayerBridge(
  kohii: Kohii,
  private val media: Media,
  private val playerProvider: PlayerProvider,
  mediaSourceFactoryProvider: MediaSourceFactoryProvider
) : PlayerEventListener, Bridge<PlayerView>, ErrorMessageProvider<ExoPlaybackException> {

  companion object {
    internal fun isBehindLiveWindow(error: ExoPlaybackException?): Boolean {
      if (error?.type != ExoPlaybackException.TYPE_SOURCE) return false
      var cause: Throwable? = error.sourceException
      while (cause != null) {
        if (cause is BehindLiveWindowException) return true
        cause = cause.cause
      }
      return false
    }
  }

  private val context = kohii.app
  private val mediaSourceFactory = mediaSourceFactoryProvider.provideMediaSourceFactory(media)

  protected val eventListeners by lazy { PlayerEventListeners() } // Set, so no duplicated
  protected val volumeListeners by lazy { VolumeChangedListeners() } // Set, so no duplicated
  protected val errorListeners by lazy { ErrorListeners() } // Set, so no duplicated

  private var listenerApplied = AtomicBoolean(false)
  private var sourcePrepared = AtomicBoolean(false)

  private var _playbackInfo = PlaybackInfo() // Backing field for PlaybackInfo set/get
  private var _repeatMode = Playable.REPEAT_MODE_OFF // Backing field
  private var _playbackParams = PlaybackParameters.DEFAULT // Backing field

  protected var mediaSource: MediaSource? = null
  protected var player: Player? = null

  private var lastSeenTrackGroupArray: TrackGroupArray? = null
  private var inErrorState = false

  override fun prepare(loadSource: Boolean) {
    this.addEventListener(this)

    if (player == null) {
      sourcePrepared.set(false)
      listenerApplied.set(false)
      // player = playerProvider.acquirePlayer(this.media)
    }

    if (loadSource) {
      prepareMediaSource()
      ensurePlayerView()
    }

    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override var playerView: PlayerView? = null
    set(value) {
      if (field == value) return // same reference
      this.lastSeenTrackGroupArray = null
      this.inErrorState = false
      if (value == null) {
        field?.let {
          // 'field' must be not null here
          it.player = null
          it.setErrorMessageProvider(null)
        }
      } else {
        this.player?.let {
          PlayerView.switchTargetView(it, field, value)
        }
      }

      field = value
      field?.setErrorMessageProvider(this)
    }

  override fun play() {
    prepareMediaSource()
    requireNotNull(player) { "Bridge#play(): Player is null!" }
    ensurePlayerView()
    player!!.playWhenReady = true
  }

  override fun pause() {
    if (sourcePrepared.get()) player?.playWhenReady = false
  }

  override fun reset() {
    playbackInfo.reset()
    player?.let {
      it.setVolumeInfo(VolumeInfo(false, 1.0F))
      it.stop(true)
    }
    this.mediaSource = null // So it will be re-prepared later.
    this.sourcePrepared.set(false)
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override fun release() {
    this.removeEventListener(this)
    this.playerView = null
    player?.let {
      if (listenerApplied.compareAndSet(true, false)) {
        it.removeEventListener(eventListeners)
      }
      (it as? VolumeInfoController)?.removeVolumeChangedListener(volumeListeners)
      it.stop(true)
      playerProvider.releasePlayer(this.media, it)
    }

    this.player = null
    this.mediaSource = null
    this.sourcePrepared.set(false)
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
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

  override val isPlaying: Boolean
    get() = player?.playWhenReady ?: false

  override val volumeInfo: VolumeInfo
    get() = this.playbackInfo.volumeInfo // this will first update the PlaybackInfo via getter.

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val changed = playbackInfo.volumeInfo !== volumeInfo // Compare value.
    if (changed) {
      playbackInfo.volumeInfo = volumeInfo
      player?.setVolumeInfo(playbackInfo.volumeInfo)
    }
    return changed
  }

  override var parameters: PlaybackParameters
    get() = _playbackParams
    set(value) {
      _playbackParams = value
      player?.playbackParameters = value
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

      player?.let {
        it.setVolumeInfo(_playbackInfo.volumeInfo)
        val haveResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
        if (haveResumePosition) {
          it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
        }
      }
    }

  override var repeatMode: Int
    get() = _repeatMode
    set(value) {
      _repeatMode = value
      this.player?.let { it.repeatMode = value }
    }

  private fun updatePlaybackInfo() {
    player?.let {
      if (it.playbackState == Player.STATE_IDLE) return
      _playbackInfo.resumeWindow = it.currentWindowIndex
      _playbackInfo.resumePosition =
        if (it.isCurrentWindowSeekable) Math.max(0, it.currentPosition) else TIME_UNSET
      _playbackInfo.volumeInfo = it.getVolumeInfo()
    }
  }

  private fun ensurePlayerView() {
    playerView?.let { if (it.player != this.player) it.player = this.player }
  }

  private fun prepareMediaSource() {
    // Note: we allow subclass to create MediaSource on demand. So it can be not-null here.
    // The flag sourcePrepared can also be set to false somewhere else.
    if (mediaSource == null) {
      sourcePrepared.set(false)
      mediaSource = mediaSourceFactory.createMediaSource(this.media.uri)
    }

    if (!sourcePrepared.get()) {
      ensurePlayer()
      (player as? ExoPlayer)?.let {
        it.prepare(mediaSource, playbackInfo.resumeWindow == INDEX_UNSET, false)
        sourcePrepared.set(true)
      }
    }
  }

  private fun ensurePlayer() {
    if (player == null) {
      sourcePrepared.set(false)
      listenerApplied.set(false)
      player = playerProvider.acquirePlayer(this.media)
    }

    player!!.let {
      if (!listenerApplied.get()) {
        (player as? VolumeInfoController)?.addVolumeChangedListener(volumeListeners)
        it.addEventListener(eventListeners)
        listenerApplied.set(true)
      }

      it.playbackParameters = _playbackParams
      val hasResumePosition = _playbackInfo.resumeWindow != PlaybackInfo.INDEX_UNSET
      if (hasResumePosition) {
        it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
      }
      it.setVolumeInfo(_playbackInfo.volumeInfo)
      it.repeatMode = _repeatMode
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected fun onErrorMessage(message: String) {
    // Sub class can have custom reaction about the error here, including not to show this toast
    // (by not calling super.onErrorMessage(message)).
    if (this.errorListeners.size > 0) {
      this.errorListeners.onError(RuntimeException(message))
    } else if (playerView != null) {
      Toast.makeText(playerView!!.context.applicationContext, message, Toast.LENGTH_SHORT)
          .show()
    }
  }

  /// ErrorMessageProvider<ExoPlaybackException> ⬇︎

  override fun getErrorMessage(e: ExoPlaybackException?): Pair<Int, String> {
    var errorString = context.getString(R.string.error_generic)
    if (e?.type == ExoPlaybackException.TYPE_RENDERER) {
      val exception = e.rendererException
      if (exception is DecoderInitializationException) {
        // Special case for decoder initialization failures.
        errorString = if (exception.decoderName == null) {
          when {
            exception.cause is MediaCodecUtil.DecoderQueryException ->
              context.getString(R.string.error_querying_decoders)
            exception.secureDecoderRequired ->
              context.getString(R.string.error_no_secure_decoder, exception.mimeType)
            else -> context.getString(R.string.error_no_decoder, exception.mimeType)
          }
        } else {
          context.getString(R.string.error_instantiating_decoder, exception.decoderName)
        }
      }
    }

    return Pair.create(0, errorString)
  }

  /// DefaultEventListener ⬇︎

  override fun onPlayerError(error: ExoPlaybackException?) {
    if (playerView == null) {
      var errorString: String? = null
      if (error?.type == ExoPlaybackException.TYPE_RENDERER) {
        val exception = error.rendererException
        if (exception is DecoderInitializationException) {
          // Special case for decoder initialization failures.
          errorString = if (exception.decoderName == null) {
            when {
              exception.cause is MediaCodecUtil.DecoderQueryException ->
                context.getString(R.string.error_querying_decoders)
              exception.secureDecoderRequired ->
                context.getString(R.string.error_no_secure_decoder, exception.mimeType)
              else -> context.getString(R.string.error_no_decoder, exception.mimeType)
            }
          } else {
            context.getString(R.string.error_instantiating_decoder, exception.decoderName)
          }
        }
      }

      if (errorString != null) onErrorMessage(errorString)
    }

    inErrorState = true
    if (isBehindLiveWindow(error)) {
      reset()
    } else {
      updatePlaybackInfo()
    }
    if (error != null) this.errorListeners.onError(error)
  }

  override fun onPositionDiscontinuity(reason: Int) {
    if (inErrorState) {
      // Adapt from ExoPlayer demo.
      // "This will only occur if the user has performed a seek whilst in the error state. Update
      // the resume position so that if the user then retries, playback will resume from the
      // position to which they seek." - ExoPlayer
      updatePlaybackInfo()
    }
  }

  override fun onTracksChanged(
    trackGroups: TrackGroupArray?,
    trackSelections: TrackSelectionArray?
  ) {
    if (trackGroups === lastSeenTrackGroupArray) return
    lastSeenTrackGroupArray = trackGroups
    val trackSelector = (playerProvider as? DefaultPlayerProvider)?.trackSelector
        as? MappingTrackSelector ?: return
    val trackInfo = trackSelector.currentMappedTrackInfo
    if (trackInfo != null) {
      if (trackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_video))
      }

      if (trackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_audio))
      }
    }
  }
}