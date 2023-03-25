/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.exoplayer

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.AbstractBridge
import kohii.v1.core.DefaultTrackSelectorHolder
import kohii.v1.core.PlayerParameters
import kohii.v1.core.PlayerPool
import kohii.v1.core.VolumeInfoController
import kohii.v1.exoplayer.internal.getVolumeInfo
import kohii.v1.exoplayer.internal.setVolumeInfo
import kohii.v1.logError
import kohii.v1.logInfo
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.PlaybackInfo.Companion.INDEX_UNSET
import kohii.v1.media.VolumeInfo
import kotlin.math.max

/**
 * @author eneim (2018/06/24).
 */
open class PlayerViewBridge(
  context: Context,
  protected val media: Media,
  protected val playerPool: PlayerPool<Player>,
  private val mediaSourceFactory: MediaSourceFactory
) : AbstractBridge<PlayerView>(), Player.Listener {

  protected open val mediaItem: MediaItem = MediaItem.fromUri(media.uri)

  private val context = context.applicationContext

  private var listenerApplied = false
  private var sourcePrepared = false

  private var _playbackInfo = PlaybackInfo() // Backing field for PlaybackInfo set/get
  private var _repeatMode = Player.REPEAT_MODE_OFF // Backing field
  private var _playbackParams = PlaybackParameters.DEFAULT // Backing field
  private var mediaSource: MediaSource? = null

  private var lastSeenTrackGroupArray: List<Tracks.Group>? = null
  private var inErrorState = false

  protected var player: Player? = null

  override val playerState: Int
    get() = player?.playbackState ?: Player.STATE_IDLE

  override var repeatMode: Int
    get() = _repeatMode
    set(value) {
      _repeatMode = value
      player?.also { it.repeatMode = value }
    }

  override fun prepare(loadSource: Boolean) {
    "Bridge#prepare loadSource=$loadSource, $this".logInfo()
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
      "Bridge#renderer $field -> $value, $this".logInfo()
      this.lastSeenTrackGroupArray = null
      this.inErrorState = false
      if (value == null) {
        requireNotNull(field).also {
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
    }

  override fun ready() {
    "Bridge#ready, $this".logInfo()
    prepareMediaSource()
    requireNotNull(player) { "Player must be available." }
    ensurePlayerView()
  }

  override fun play() {
    super.play()
    if (playerParameters.playerShouldStart()) {
      requireNotNull(player).playWhenReady = true
    }
  }

  override fun pause() {
    super.pause()
    if (sourcePrepared) player?.playWhenReady = false
  }

  override fun reset(resetPlayer: Boolean) {
    "Bridge#reset resetPlayer=$resetPlayer, $this".logInfo()
    if (resetPlayer) _playbackInfo = PlaybackInfo()
    else updatePlaybackInfo()
    player?.also {
      it.setVolumeInfo(VolumeInfo.DEFAULT_ACTIVE)
      it.stop()
      if (resetPlayer) it.clearMediaItems()
    }
    this.mediaSource = null // So it will be re-prepared later.
    this.sourcePrepared = false
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override fun release() {
    "Bridge#release, $this".logInfo()
    this.removeEventListener(this)
    // this.playerView = null // Bridge's owner must do this.
    this.renderer?.player = null
    _playbackInfo = PlaybackInfo()
    player?.also {
      if (listenerApplied) {
        it.removeListener(eventListeners)
        listenerApplied = false
      }
      (it as? VolumeInfoController)?.removeVolumeChangedListener(volumeListeners)
      it.stop()
      it.clearMediaItems()
      playerPool.putPlayer(this.media, it)
    }

    this.player = null
    this.mediaSource = null
    this.sourcePrepared = false
    this.lastSeenTrackGroupArray = null
    this.inErrorState = false
  }

  override fun isPlaying(): Boolean {
    return player?.run {
      playWhenReady &&
          playbackState in Player.STATE_BUFFERING..Player.STATE_READY &&
          playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE
    } ?: false
  }

  override var volumeInfo: VolumeInfo = player?.getVolumeInfo() ?: VolumeInfo.DEFAULT_ACTIVE
    set(value) {
      "Bridge#volumeInfo $field -> $value, $this".logInfo()
      if (field == value) return
      field = value
      player?.setVolumeInfo(value)
    }

  override fun seekTo(positionMs: Long) {
    val playbackInfo = this.playbackInfo
    playbackInfo.resumePosition = positionMs
    playbackInfo.resumeWindow = player?.currentWindowIndex ?: playbackInfo.resumeWindow
    this.playbackInfo = playbackInfo
  }

  override var playbackInfo: PlaybackInfo
    get() {
      updatePlaybackInfo()
      return _playbackInfo
    }
    set(value) {
      this.setPlaybackInfo(value, false)
    }

  override var playerParameters: PlayerParameters = PlayerParameters()
    set(value) {
      field = value
      applyPlayerParameters(value)
    }

  private fun applyPlayerParameters(parameters: PlayerParameters) {
    val player = this.player
    if (player is DefaultTrackSelectorHolder) {
      player.trackSelector.parameters = player.trackSelector.parameters.buildUpon()
          .setMaxVideoSize(parameters.maxVideoWidth, parameters.maxVideoHeight)
          .setMaxVideoBitrate(parameters.maxVideoBitrate)
          .setMaxAudioBitrate(parameters.maxAudioBitrate)
          .build()
    }
  }

  private fun setPlaybackInfo(
    playbackInfo: PlaybackInfo,
    volumeOnly: Boolean
  ) {
    _playbackInfo = playbackInfo

    player?.also {
      if (!volumeOnly) {
        val haveResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
        if (haveResumePosition) {
          it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
        }
      }
    }
  }

  private fun updatePlaybackInfo() {
    player?.also {
      if (it.playbackState == Player.STATE_IDLE) return
      _playbackInfo = PlaybackInfo(
          it.currentWindowIndex,
          max(0, it.currentPosition)
      )
    }
  }

  private fun ensurePlayerView() {
    renderer?.also { if (it.player !== this.player) it.player = this.player }
  }

  private fun prepareMediaSource() {
    val mediaSource: MediaSource = this.mediaSource ?: run {
      sourcePrepared = false
      mediaSourceFactory.createMediaSource(mediaItem).also { this.mediaSource = it }
    }

    // Player was reset, need to prepare again.
    if (player?.playbackState == Player.STATE_IDLE) {
      sourcePrepared = false
    }

    if (!sourcePrepared) {
      ensurePlayer()
      (player as? ExoPlayer)?.also {
        it.setMediaSource(mediaSource, /* resetPosition */ playbackInfo.resumeWindow == INDEX_UNSET)
        it.prepare()
        sourcePrepared = true
      }
    }
  }

  private fun ensurePlayer() {
    if (player == null) {
      sourcePrepared = false
      listenerApplied = false
      val player = playerPool.getPlayer(this.media)
      applyPlayerParameters(playerParameters)
      this.player = player
    }

    requireNotNull(player).also {
      if (!listenerApplied) {
        (player as? VolumeInfoController)?.addVolumeChangedListener(volumeListeners)
        it.addListener(eventListeners)
        listenerApplied = true
      }

      it.playbackParameters = _playbackParams
      val hasResumePosition = _playbackInfo.resumeWindow != INDEX_UNSET
      if (hasResumePosition) {
        it.seekTo(_playbackInfo.resumeWindow, _playbackInfo.resumePosition)
      }
      it.setVolumeInfo(volumeInfo)
      it.repeatMode = _repeatMode
    }
  }

  private fun onErrorMessage(
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

  //region Player.Listener implementation
  override fun onPlayerError(error: PlaybackException) {
    "Bridge#onPlayerError error=${error.cause}, message=${error.cause?.message}, $this".logError()
    if (renderer == null) {
      var errorString: String? = null
      val exception = error.cause
      if (exception is DecoderInitializationException) {
        // Special case for decoder initialization failures.
        errorString = if (exception.codecInfo == null) {
          when {
            exception.cause is MediaCodecUtil.DecoderQueryException ->
              context.getString(R.string.error_querying_decoders)

            exception.secureDecoderRequired ->
              context.getString(R.string.error_no_secure_decoder, exception.mimeType)

            else -> context.getString(R.string.error_no_decoder, exception.mimeType)
          }
        } else {
          context.getString(R.string.error_instantiating_decoder, exception.codecInfo?.name ?: "")
        }
      }

      if (errorString != null) onErrorMessage(errorString, error)
    }

    inErrorState = true
    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
      reset()
    } else {
      updatePlaybackInfo()
    }
    this.errorListeners.onError(error)
  }

  @Deprecated("Deprecated in Java")
  override fun onPositionDiscontinuity(reason: Int) {
    if (inErrorState) {
      // Adapt from ExoPlayer demo.
      // "This will only occur if the user has performed a seek whilst in the error state. Update
      // the resume position so that if the user then retries, playback will resume from the
      // position to which they seek." - ExoPlayer
      updatePlaybackInfo()
    }
  }

  override fun onTracksChanged(tracks: Tracks) {
    val groups = tracks.groups
    if (groups == lastSeenTrackGroupArray) return
    lastSeenTrackGroupArray = groups
    val player = this.player as? KohiiExoPlayer ?: return
    val trackInfo = player.trackSelector.currentMappedTrackInfo
    if (trackInfo != null) {
      if (trackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_video), player.playerError)
      }

      if (trackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        onErrorMessage(context.getString(R.string.error_unsupported_audio), player.playerError)
      }
    }
  }

  //endregion
}
