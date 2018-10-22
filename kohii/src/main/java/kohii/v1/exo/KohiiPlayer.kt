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
import androidx.annotation.CallSuper
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import kohii.media.VolumeInfo
import kohii.v1.VolumeChangedListener
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author eneim (2018/06/25).
 */
open class KohiiPlayer(
    context: Context,
    renderersFactory: RenderersFactory,
    trackSelector: TrackSelector,
    loadControl: LoadControl,
    bandwidthMeter: BandwidthMeter,
    drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?
) : SimpleExoPlayer(context, renderersFactory, trackSelector, loadControl, bandwidthMeter,
    drmSessionManager, Looper.myLooper()) {

  private var volumeChangedListeners: CopyOnWriteArraySet<VolumeChangedListener>? = null

  val volumeInfo = VolumeInfo(false, 1f)

  @CallSuper
  override fun setVolume(audioVolume: Float) {
    this.setVolumeInfo(VolumeInfo(audioVolume == 0f, audioVolume))
  }

  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    val changed = this.volumeInfo != volumeInfo
    if (changed) {
      this.volumeInfo.setTo(volumeInfo.mute, volumeInfo.volume)
      super.setVolume(if (volumeInfo.mute) 0F else volumeInfo.volume)
      this.volumeChangedListeners?.forEach { it.onVolumeChanged(volumeInfo) }
    }
    return changed
  }

  fun addOnVolumeChangedListener(listener: VolumeChangedListener) {
    if (volumeChangedListeners == null) volumeChangedListeners = CopyOnWriteArraySet()
    volumeChangedListeners!!.add(listener)
  }

  fun removeOnVolumeChangedListener(listener: VolumeChangedListener?) {
    if (volumeChangedListeners != null) volumeChangedListeners!!.remove(listener)
  }

  fun clearOnVolumeChangedListener() {
    if (volumeChangedListeners != null) volumeChangedListeners!!.clear()
  }
}
