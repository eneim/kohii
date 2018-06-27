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

package kohii.v1

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo

/**
 * @author eneim (2018/06/24).
 */
internal interface Helper {

  var playerView: PlayerView?

  var playbackInfo: PlaybackInfo

  val isPlaying: Boolean

  val volumeInfo: VolumeInfo

  var parameters: PlaybackParameters?

  /**
   * Prepare the resource for a [ExoPlayer]. This method should:
   * - Request for new [ExoPlayer] instance if there is not a usable one.
   * - Configure [PlayerEventListener] for it.
   * - If there is non-trivial PlaybackInfo, update it to the SimpleExoPlayer.
   * - If client request to prepare MediaSource, then prepare it.
   *
   * This method must be called before [.setPlayerView].
   *
   * @param loadSource if `true`, also prepare the MediaSource when preparing the Player,
   * if `false` just do nothing for the MediaSource.
   */
  fun prepare(loadSource: Boolean)

  fun play()

  fun pause()

  /**
   * Reset all resource, so that the playback can start all over again. This is to cleanup the
   * playback for reuse. The SimpleExoPlayer instance must be still usable without calling
   * [prepare].
   */
  fun reset()

  /**
   * Release all resource. After this, the SimpleExoPlayer is released to the Player pool and the
   * Playable must call [prepare] again to use it again.
   */
  fun release()

  /**
   * Add a new [PlayerEventListener] to this Playable. As calling [prepare] also triggers some
   * internal events, this method should be called before [prepare] so that Client could received
   * them all.
   *
   * @param listener the EventListener to add, must be not `null`.
   */
  fun addEventListener(listener: PlayerEventListener)

  /**
   * Remove an [PlayerEventListener] from this Playable.
   *
   * @param listener the EventListener to be removed. If null, nothing happens.
   */
  fun removeEventListener(listener: PlayerEventListener?)

  fun addOnVolumeChangeListener(listener: OnVolumeChangedListener)

  fun removeOnVolumeChangeListener(listener: OnVolumeChangedListener?)

  /**
   * Update playback's volume.
   *
   * @param volumeInfo the [VolumeInfo] to update to.
   * @return `true` if current Volume info is updated, `false` otherwise.
   */
  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  class PlayerEventListeners : HashSet<PlayerEventListener>(), PlayerEventListener {
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
      this.forEach { it.onPlaybackParametersChanged(playbackParameters) }
    }

    override fun onSeekProcessed() {
      this.forEach { it.onSeekProcessed() }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?) {
      this.forEach { it.onTracksChanged(trackGroups, trackSelections) }
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
      this.forEach { it.onPlayerError(error) }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
      this.forEach { it.onLoadingChanged(isLoading) }
    }

    override fun onPositionDiscontinuity(reason: Int) {
      this.forEach { it.onPositionDiscontinuity(reason) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
      this.forEach { it.onRepeatModeChanged(repeatMode) }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
      this.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
      this.forEach { it.onTimelineChanged(timeline, manifest, reason) }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      this.forEach { it.onPlayerStateChanged(playWhenReady, playbackState) }
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float) {
      this.forEach {
        it.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
      }
    }

    override fun onRenderedFirstFrame() {
      this.forEach { it.onRenderedFirstFrame() }
    }

    override fun onCues(cues: MutableList<Cue>?) {
      this.forEach { it.onCues(cues) }
    }

    override fun onMetadata(metadata: Metadata?) {
      this.forEach { it.onMetadata(metadata) }
    }
  }
}
