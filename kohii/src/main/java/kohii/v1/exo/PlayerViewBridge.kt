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

import android.content.Context
import android.util.Log
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
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kohii.v1.addEventListener
import kohii.core.Common
import kohii.v1.getVolumeInfo
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.PlaybackInfo.Companion.INDEX_UNSET
import kohii.media.PlaybackInfo.Companion.TIME_UNSET
import kohii.media.VolumeInfo
import kohii.v1.removeEventListener
import kohii.v1.setVolumeInfo
import kohii.v1.BaseBridge
import kohii.v1.PlayerEventListener
import kohii.v1.R
import kohii.v1.VolumeInfoController
import kotlin.math.max

/**
 * @author eneim (2018/06/24).
 */
internal open class PlayerViewBridge(
  context: Context,
  private val media: Media,
  private val playerProvider: ExoPlayerProvider,
  mediaSourceFactoryProvider: MediaSourceFactoryProvider
) : BaseBridge<PlayerView>(), PlayerEventListener, ErrorMessageProvider<ExoPlaybackException> {

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

  private val context = context.applicationContext
  private val mediaSourceFactory = mediaSourceFactoryProvider.provideMediaSourceFactory(media)

  private var listenerApplied = false
  private var sourcePrepared = false

  private var _playbackInfo = PlaybackInfo() // Backing field for PlaybackInfo set/get
  private var _repeatMode = Common.REPEAT_MODE_OFF // Backing field
  private var _playbackParams = PlaybackParameters.DEFAULT // Backing field
  private var mediaSource: MediaSource? = null

  private var lastSeenTrackGroupArray: TrackGroupArray? = null
  private var inErrorState = false

  internal var player: Player? = null

  override val playbackState: Int
    get() = player?.playbackState ?: -1

  override fun prepare(loadSource: Boolean) {
    super.addEventListener(this)

    if (player == null) {
      sourcePrepared = false
      listenerApplied = false
    }

    if (loadSource) {
      prepareMediaSource()
      ensurePlayerView()
    }

    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override var renderer: PlayerView? = null
    set(value) {
      if (field === value) return // same reference
      this.lastSeenTrackGroupArray = null
      this.inErrorState = false
      if (value == null) {
        field!!.also {
          // 'field' must be not null here
          it.player = null
          it.setErrorMessageProvider(null)
        }
      } else {
        this.player?.also {
          PlayerView.switchTargetView(it, field, value)
        }
      }

      field = value
      field?.setErrorMessageProvider(this)
    }

  override fun ready() {
    prepareMediaSource()
    requireNotNull(player) { "Player must be available." }
    ensurePlayerView()
  }

  override fun play() {
    requireNotNull(player).playWhenReady = true
  }

  override fun pause() {
    if (sourcePrepared) player?.playWhenReady = false
  }

  override fun reset(resetPlayer: Boolean) {
    if (resetPlayer) _playbackInfo = PlaybackInfo()
    else updatePlaybackInfo()
    player?.also {
      it.setVolumeInfo(VolumeInfo(false, 1.0F))
      it.stop(resetPlayer)
    }
    this.mediaSource = null // So it will be re-prepared later.
    this.sourcePrepared = false
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override fun release() {
    this.removeEventListener(this)
    // this.playerView = null // Bridge's owner must do this.
    this.renderer?.player = null
    _playbackInfo = PlaybackInfo()
    player?.also {
      if (listenerApplied) {
        it.removeEventListener(eventListeners)
        listenerApplied = false
      }
      (it as? VolumeInfoController)?.removeVolumeChangedListener(volumeListeners)
      it.stop(true)
      playerProvider.releasePlayer(this.media, it)
    }

    this.player = null
    this.mediaSource = null
    this.sourcePrepared = false
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  // TODO double check this once ExoPlayer release the "Player.isPlaying" API
  override fun isPlaying(): Boolean {
    return player?.let {
      it.playbackState in 2..3 && it.playWhenReady
    } ?: false
  }

  override val volumeInfo: VolumeInfo
    get() = this.playbackInfo.volumeInfo // this will first update the PlaybackInfo via getter.

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val current = this.playbackInfo
    val changed = current.volumeInfo != volumeInfo // Compare value.
    if (changed) {
      current.volumeInfo = volumeInfo
      this.setPlaybackInfo(current, true)
    }
    return changed
  }

  override fun seekTo(positionMs: Long) {
    val playbackInfo = this.playbackInfo
    playbackInfo.resumePosition = positionMs
    playbackInfo.resumeWindow = player?.currentWindowIndex ?: playbackInfo.resumeWindow
    this.playbackInfo = playbackInfo
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
      this.setPlaybackInfo(value, false)
    }

  private fun setPlaybackInfo(
    playbackInfo: PlaybackInfo,
    volumeOnly: Boolean
  ) {
    _playbackInfo = playbackInfo

    player?.also {
      it.setVolumeInfo(_playbackInfo.volumeInfo)
      if (!volumeOnly) {
        val haveResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
        if (haveResumePosition) {
          it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
        }
      }
    }
  }

  override var repeatMode: Int
    get() = _repeatMode
    set(value) {
      _repeatMode = value
      this.player?.also { it.repeatMode = value }
    }

  private fun updatePlaybackInfo() {
    player?.also {
      if (it.playbackState == Common.STATE_IDLE) return
      _playbackInfo = PlaybackInfo(
          it.currentWindowIndex,
          if (it.isCurrentWindowSeekable) max(0, it.currentPosition) else TIME_UNSET,
          it.getVolumeInfo()
      )
    }
  }

  private fun ensurePlayerView() {
    renderer?.also { if (it.player !== this.player) it.player = this.player }
  }

  private fun prepareMediaSource() {
    // Note: we allow subclass to create MediaSource on demand. So it can be not-null here.
    // The flag sourcePrepared can also be set to false somewhere else.
    if (mediaSource == null) {
      sourcePrepared = false
      mediaSource = mediaSourceFactory.createMediaSource(this.media.uri)
    }

    // Player is reset, need to prepare again.
    if (player?.playbackState == Common.STATE_IDLE) {
      sourcePrepared = false
    }

    if (!sourcePrepared) {
      ensurePlayer()
      (player as? ExoPlayer)?.also {
        it.prepare(mediaSource, playbackInfo.resumeWindow == INDEX_UNSET, false)
        sourcePrepared = true
      }
    }
  }

  private fun ensurePlayer() {
    if (player == null) {
      sourcePrepared = false
      listenerApplied = false
      player = playerProvider.acquirePlayer(this.media)
    }

    requireNotNull(player).also {
      if (!listenerApplied) {
        (player as? VolumeInfoController)?.addVolumeChangedListener(volumeListeners)
        it.addEventListener(eventListeners)
        listenerApplied = true
      }

      it.playbackParameters = _playbackParams
      val hasResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
      if (hasResumePosition) {
        it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
      }
      it.setVolumeInfo(_playbackInfo.volumeInfo)
      it.repeatMode = _repeatMode
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected fun onErrorMessage(
    message: String,
    cause: Throwable?
  ) {
    // Sub class can have custom reaction about the error here, including not to show this toast
    // (by not calling super.onErrorMessage(message)).
    if (this.errorListeners.isNotEmpty()) {
      this.errorListeners.onError(RuntimeException(message, cause))
    } else {
      Toast.makeText(context, message, Toast.LENGTH_SHORT)
          .show()
    }
  }

  // ErrorMessageProvider<ExoPlaybackException> ⬇︎

  override fun getErrorMessage(e: ExoPlaybackException?): Pair<Int, String> {
    Log.e("Kohii::Bridge", "Error: ${e?.cause}")
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

  // DefaultEventListener ⬇︎

  override fun onPlayerError(error: ExoPlaybackException?) {
    Log.e("Kohii::Bridge", "Error: ${error?.cause}")
    if (renderer == null) {
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

      if (errorString != null) onErrorMessage(errorString, error)
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
    if (trackGroups == lastSeenTrackGroupArray) return
    lastSeenTrackGroupArray = trackGroups
    val player = this.player as? KohiiExoPlayer ?: return
    val trackInfo = player.trackSelector.currentMappedTrackInfo
    if (trackInfo != null) {
      if (trackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_video), player.playbackError)
      }

      if (trackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_audio), player.playbackError)
      }
    }
  }
}
