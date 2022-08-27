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

import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.PositionInfo
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.video.VideoSize
import kohii.v1.media.VolumeInfo
import java.util.concurrent.CopyOnWriteArraySet

interface Prioritized : Comparable<Prioritized> {

  override fun compareTo(other: Prioritized): Int = 0
}

@Deprecated("Use Player.Listener instead.")
interface PlayerEventListener : Player.Listener

interface VolumeChangedListener {

  fun onVolumeChanged(volumeInfo: VolumeInfo)
}

interface ErrorListener {

  fun onError(error: Exception)
}

class PlayerEventListeners : CopyOnWriteArraySet<Player.Listener>(), Player.Listener {

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters): Unit =
    forEach { it.onPlaybackParametersChanged(playbackParameters) }

  @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
  @Deprecated("Deprecated in Java")
  override fun onTracksChanged(
    trackGroups: TrackGroupArray,
    trackSelections: TrackSelectionArray
  ): Unit = forEach { it.onTracksChanged(trackGroups, trackSelections) }

  override fun onPlayerError(error: PlaybackException): Unit =
    forEach { it.onPlayerError(error) }

  override fun onIsLoadingChanged(isLoading: Boolean): Unit =
    forEach { it.onIsLoadingChanged(isLoading) }

  override fun onPositionDiscontinuity(
    oldPosition: PositionInfo,
    newPosition: PositionInfo,
    reason: Int
  ): Unit = forEach { it.onPositionDiscontinuity(oldPosition, newPosition, reason) }

  override fun onRepeatModeChanged(repeatMode: Int): Unit =
    forEach { it.onRepeatModeChanged(repeatMode) }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean): Unit =
    forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }

  override fun onTimelineChanged(timeline: Timeline, reason: Int): Unit =
    forEach { it.onTimelineChanged(timeline, reason) }

  override fun onPlaybackStateChanged(state: Int): Unit =
    forEach { it.onPlaybackStateChanged(state) }

  override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int): Unit =
    forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }

  // Keep this to deliver the events to Playback.
  @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
  @Deprecated("Deprecated in Java")
  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    forEach { it.onPlayerStateChanged(playWhenReady, playbackState) }
  }

  override fun onVideoSizeChanged(videoSize: VideoSize): Unit =
    forEach { it.onVideoSizeChanged(videoSize) }

  override fun onRenderedFirstFrame(): Unit = forEach { it.onRenderedFirstFrame() }

  override fun onCues(cues: MutableList<Cue>): Unit = forEach { it.onCues(cues) }

  override fun onMetadata(metadata: Metadata): Unit = forEach { it.onMetadata(metadata) }

  override fun onAudioAttributesChanged(audioAttributes: AudioAttributes): Unit =
    forEach { it.onAudioAttributesChanged(audioAttributes) }

  override fun onVolumeChanged(volume: Float): Unit = forEach { it.onVolumeChanged(volume) }

  override fun onAudioSessionIdChanged(audioSessionId: Int): Unit =
    forEach { it.onAudioSessionIdChanged(audioSessionId) }
}

class VolumeChangedListeners : CopyOnWriteArraySet<VolumeChangedListener>(), VolumeChangedListener {
  override fun onVolumeChanged(volumeInfo: VolumeInfo): Unit =
    forEach { it.onVolumeChanged(volumeInfo) }
}

class ErrorListeners : CopyOnWriteArraySet<ErrorListener>(), ErrorListener {
  override fun onError(error: Exception): Unit = forEach { it.onError(error) }
}

interface VolumeInfoController {

  val volumeInfo: VolumeInfo

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean

  fun addVolumeChangedListener(listener: VolumeChangedListener)

  fun removeVolumeChangedListener(listener: VolumeChangedListener?)
}

// TODO(eneim): document about the usage of this interface.
interface DefaultTrackSelectorHolder {

  val trackSelector: DefaultTrackSelector
}

interface PlayableManager

interface PlayableContainer
