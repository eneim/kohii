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

package kohii.v1.core

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player.EventListener
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.video.VideoListener
import kohii.v1.media.VolumeInfo
import java.util.concurrent.CopyOnWriteArraySet

interface Prioritized : Comparable<Prioritized> {

  @JvmDefault
  override fun compareTo(other: Prioritized): Int {
    return 0
  }
}

interface PlayerEventListener : EventListener,
    VideoListener,
    AudioListener,
    TextOutput,
    MetadataOutput {

  @JvmDefault
  override fun onCues(cues: MutableList<Cue>?) {
  }

  @JvmDefault
  override fun onMetadata(metadata: Metadata?) {
  }
}

interface VolumeChangedListener {

  fun onVolumeChanged(volumeInfo: VolumeInfo)
}

interface ErrorListener {

  fun onError(error: Exception)
}

class PlayerEventListeners : CopyOnWriteArraySet<PlayerEventListener>(),
    PlayerEventListener {

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    this.forEach { it.onPlaybackParametersChanged(playbackParameters) }
  }

  override fun onSeekProcessed() {
    this.forEach { it.onSeekProcessed() }
  }

  override fun onTracksChanged(
    trackGroups: TrackGroupArray?,
    trackSelections: TrackSelectionArray?
  ) {
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

  override fun onTimelineChanged(
    timeline: Timeline?,
    manifest: Any?,
    reason: Int
  ) {
    this.forEach { it.onTimelineChanged(timeline, manifest, reason) }
  }

  override fun onPlayerStateChanged(
    playWhenReady: Boolean,
    playbackState: Int
  ) {
    this.forEach { it.onPlayerStateChanged(playWhenReady, playbackState) }
  }

  override fun onVideoSizeChanged(
    width: Int,
    height: Int,
    unappliedRotationDegrees: Int,
    pixelWidthHeightRatio: Float
  ) {
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

  override fun onAudioAttributesChanged(audioAttributes: AudioAttributes?) {
    this.forEach { it.onAudioAttributesChanged(audioAttributes) }
  }

  override fun onVolumeChanged(volume: Float) {
    this.forEach { it.onVolumeChanged(volume) }
  }

  override fun onAudioSessionId(audioSessionId: Int) {
    this.forEach { it.onAudioSessionId(audioSessionId) }
  }
}

class VolumeChangedListeners : CopyOnWriteArraySet<VolumeChangedListener>(),
    VolumeChangedListener {
  override fun onVolumeChanged(volumeInfo: VolumeInfo) {
    this.forEach { it.onVolumeChanged(volumeInfo) }
  }
}

class ErrorListeners : CopyOnWriteArraySet<ErrorListener>(),
    ErrorListener {
  override fun onError(error: Exception) {
    this.forEach { it.onError(error) }
  }
}

interface VolumeInfoController {

  val volumeInfo: VolumeInfo

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  fun addVolumeChangedListener(listener: VolumeChangedListener)

  fun removeVolumeChangedListener(listener: VolumeChangedListener?)
}

interface PlayableManager
