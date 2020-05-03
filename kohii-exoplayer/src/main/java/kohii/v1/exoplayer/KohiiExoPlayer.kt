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
import android.os.Looper
import androidx.annotation.CallSuper
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util
import kohii.v1.core.DefaultTrackSelectorHolder
import kohii.v1.core.VolumeChangedListener
import kohii.v1.core.VolumeChangedListeners
import kohii.v1.core.VolumeInfoController
import kohii.v1.media.VolumeInfo
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Extend the [SimpleExoPlayer] to have custom configuration.
 *
 * @author eneim (2018/06/25).
 */
open class KohiiExoPlayer(
  context: Context,
  renderersFactory: RenderersFactory = DefaultRenderersFactory(
      context.applicationContext
  ).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF),
    // TrackSelector is initialized at the same time a new Player instance is created.
    // This process will set the BandwidthMeter to the TrackSelector. Therefore we need to have
    // unique TrackSelector per Player instance.
  override val trackSelector: DefaultTrackSelector = DefaultTrackSelector(context.applicationContext),
  loadControl: LoadControl = DefaultLoadControl(),
  bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.Builder(context.applicationContext)
      .build(),
  looper: Looper = Util.getLooper()
) : SimpleExoPlayer(
    context,
    renderersFactory,
    trackSelector,
    loadControl,
    bandwidthMeter,
    AnalyticsCollector(Clock.DEFAULT),
    Clock.DEFAULT,
    looper
), VolumeInfoController, DefaultTrackSelectorHolder {

  private val volumeChangedListeners by lazy(NONE) { VolumeChangedListeners() }
  private var playerVolumeInfo = VolumeInfo(false, 1.0F) // backing field.

  override val volumeInfo
    get() = playerVolumeInfo

  @CallSuper
  override fun setVolume(audioVolume: Float) {
    this.setVolumeInfo(VolumeInfo(audioVolume == 0f, audioVolume))
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val changed = this.playerVolumeInfo != volumeInfo // Compare equality, not reference.
    if (changed) {
      this.playerVolumeInfo = volumeInfo
      super.setVolume(if (volumeInfo.mute) 0F else volumeInfo.volume)
      val mute = volumeInfo.mute || volumeInfo.volume == 0F
      super.setAudioAttributes(super.getAudioAttributes(), !mute)
      this.volumeChangedListeners.onVolumeChanged(volumeInfo)
    }
    return changed
  }

  override fun addVolumeChangedListener(listener: VolumeChangedListener) {
    volumeChangedListeners.add(listener)
  }

  override fun removeVolumeChangedListener(listener: VolumeChangedListener?) {
    volumeChangedListeners.remove(listener)
  }
}
