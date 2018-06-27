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

import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.upstream.cache.Cache
import kohii.media.MediaDrm

/**
 * Configuration for [PlayerFactory] and [MediaSourceFactory].
 *
 * @author eneim (2018/06/24).
 */
data class Config(
    @ExtensionRendererMode val extensionMode: Int,
    val mediaDrm: MediaDrm? = null,
    val cache: Cache? = null,
    val meter: DataMeter<BandwidthMeter, TransferListener<Any>> = DEFAULT_METER
) {

  companion object {
    private val meter = DefaultBandwidthMeter()
    val DEFAULT_METER = DataMeter<BandwidthMeter, TransferListener<Any>>(meter, meter)
    val DEFAULT_CONFIG = Config(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
  }
}