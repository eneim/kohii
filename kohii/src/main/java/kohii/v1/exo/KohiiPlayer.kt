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
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import kohii.media.VolumeInfo
import kohii.v1.BuildConfig
import kohii.v1.VolumeChangedListener
import kohii.v1.VolumeInfoController
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Extend the [SimpleExoPlayer] to have custom configuration.
 *
 * @author eneim (2018/06/25).
 */
open class KohiiPlayer(
  context: Context,
  renderersFactory: RenderersFactory,
  trackSelector: TrackSelector,
  loadControl: LoadControl,
  bandwidthMeter: BandwidthMeter,
  drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
  looper: Looper
) : SimpleExoPlayer( //
    context,
    renderersFactory,
    trackSelector,
    loadControl,
    bandwidthMeter,
    drmSessionManager,
    looper
), VolumeInfoController {

  companion object {
    val instanceCount = AtomicInteger(0)
  }

  init {
    if (BuildConfig.DEBUG) Log.w("Kohii:Player", "player: ${instanceCount.incrementAndGet()}")
  }

  private val volumeChangedListeners by lazy { CopyOnWriteArraySet<VolumeChangedListener>() }
  private val _volumeInfo = VolumeInfo(false, 1.0F) // backing field.

  override val volumeInfo
    get() = VolumeInfo(_volumeInfo)

  @CallSuper
  override fun setVolume(audioVolume: Float) {
    this.setVolumeInfo(VolumeInfo(audioVolume == 0f, audioVolume))
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val changed = this._volumeInfo != volumeInfo // Compare equality, not reference.
    if (changed) {
      this._volumeInfo.setTo(volumeInfo.mute, volumeInfo.volume)
      super.setVolume(if (volumeInfo.mute) 0F else volumeInfo.volume)
      this.volumeChangedListeners.forEach { it.onVolumeChanged(volumeInfo) }
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
