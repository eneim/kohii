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
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.video.VideoListener
import kohii.media.VolumeInfo

/**
 * @author eneim (2018/06/24).
 */
interface PlaybackEventListener {

  fun onBuffering()  // ExoPlayer state: 2

  fun onPlaying()  // ExoPlayer state: 3, play flag: true

  fun onPaused()   // ExoPlayer state: 3, play flag: false

  fun onCompleted()  // ExoPlayer state: 4
}

interface OnVolumeChangedListener {

  fun onVolumeChanged(volumeInfo: VolumeInfo)
}

interface OnErrorListener {

  fun onError(error: Exception)
}

interface PlayerEventListener : Player.EventListener, VideoListener, TextOutput, MetadataOutput

abstract class DefaultEventListener : PlayerEventListener {
  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

  }

  override fun onSeekProcessed() {

  }

  override fun onTracksChanged(trackGroups: TrackGroupArray?,
      trackSelections: TrackSelectionArray?) {

  }

  override fun onPlayerError(error: ExoPlaybackException?) {

  }

  override fun onLoadingChanged(isLoading: Boolean) {

  }

  override fun onPositionDiscontinuity(reason: Int) {

  }

  override fun onRepeatModeChanged(repeatMode: Int) {

  }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

  }

  override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

  }

  override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float) {

  }

  override fun onRenderedFirstFrame() {

  }

  override fun onCues(cues: MutableList<Cue>?) {

  }

  override fun onMetadata(metadata: Metadata?) {

  }

}