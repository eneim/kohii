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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.util.Clock

/**
 * Detailed config for building a [com.google.android.exoplayer2.SimpleExoPlayer]. Only for
 * advanced user.
 *
 * @see createKohii
 */
data class ExoPlayerConfig(
  internal val clock: Clock = Clock.DEFAULT,
  // DefaultTrackSelector parameters
  internal val trackSelectorParameters: Parameters = Parameters.DEFAULT_WITHOUT_CONTEXT,
  internal val trackSelectionFactory: ExoTrackSelection.Factory = AdaptiveTrackSelection.Factory(),
  // DefaultBandwidthMeter parameters
  internal val overrideInitialBitrateEstimate: Long = -1,
  internal val resetOnNetworkTypeChange: Boolean = true,
  internal val slidingWindowMaxWeight: Int = DefaultBandwidthMeter.DEFAULT_SLIDING_WINDOW_MAX_WEIGHT,
  // DefaultRenderersFactory parameters
  internal val enableDecoderFallback: Boolean = true,
  internal val allowedVideoJoiningTimeMs: Long = DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS,
  internal val extensionRendererMode: Int = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF,
  internal val mediaCodecSelector: MediaCodecSelector = MediaCodecSelector.DEFAULT,
  // DefaultLoadControl parameters
  internal val allocator: DefaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
  internal val minBufferMs: Int = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
  internal val maxBufferMs: Int = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
  internal val bufferForPlaybackMs: Int = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
  internal val bufferForPlaybackAfterRebufferMs: Int = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
  internal val prioritizeTimeOverSizeThresholds: Boolean = DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS,
  internal val targetBufferBytes: Int = DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES,
  internal val backBufferDurationMs: Int = DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS,
  internal val retainBackBufferFromKeyframe: Boolean = DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME,
  // Other configurations
  internal val cache: Cache? = null
) : LoadControlFactory, BandwidthMeterFactory, TrackSelectorFactory {

  companion object {
    /**
     * Every fields are default, following the setup by ExoPlayer.
     */
    @JvmStatic
    val DEFAULT = ExoPlayerConfig()

    /**
     * Reduce some setting for fast start playback.
     */
    @JvmStatic
    val FAST_START = ExoPlayerConfig(
      minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS / 10,
      maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS / 10,
      bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 10,
      bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 10
    )
  }

  override fun createLoadControl(): LoadControl = DefaultLoadControl.Builder()
    .setAllocator(allocator)
    .setBackBuffer(
      backBufferDurationMs,
      retainBackBufferFromKeyframe
    )
    .setBufferDurationsMs(
      minBufferMs,
      maxBufferMs,
      bufferForPlaybackMs,
      bufferForPlaybackAfterRebufferMs
    )
    .setPrioritizeTimeOverSizeThresholds(prioritizeTimeOverSizeThresholds)
    .setTargetBufferBytes(targetBufferBytes)
    .build()

  override fun createBandwidthMeter(context: Context): BandwidthMeter =
    DefaultBandwidthMeter.Builder(context.applicationContext)
      .setClock(clock)
      .setResetOnNetworkTypeChange(resetOnNetworkTypeChange)
      .setSlidingWindowMaxWeight(slidingWindowMaxWeight)
      .apply {
        if (overrideInitialBitrateEstimate > 0) {
          setInitialBitrateEstimate(overrideInitialBitrateEstimate)
        }
      }
      .build()

  override fun createDefaultTrackSelector(context: Context): DefaultTrackSelector {
    val parameters: Parameters =
      if (trackSelectorParameters === Parameters.DEFAULT_WITHOUT_CONTEXT) {
        trackSelectorParameters.buildUpon()
          .setViewportSizeToPhysicalDisplaySize(context, true)
          .build()
      } else {
        trackSelectorParameters
      }

    return DefaultTrackSelector(context, parameters, trackSelectionFactory)
  }
}

// For internal use only
fun ExoPlayerConfig.createDefaultPlayerPool(
  context: Context,
  userAgent: String
) = ExoPlayerPool(
  context = context.applicationContext,
  userAgent = userAgent,
  clock = clock,
  bandwidthMeterFactory = this,
  trackSelectorFactory = this,
  loadControlFactory = this,
  renderersFactory = DefaultRenderersFactory(context.applicationContext)
    .setEnableDecoderFallback(enableDecoderFallback)
    .setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs)
    .setExtensionRendererMode(extensionRendererMode)
    .setMediaCodecSelector(mediaCodecSelector)
)
