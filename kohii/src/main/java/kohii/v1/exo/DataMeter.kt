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

import android.os.Handler
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.BandwidthMeter.EventListener
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * Because DefaultBandwidthMeter is final ...
 *
 * @author eneim (2018/06/25).
 */
@Suppress("UNCHECKED_CAST")
open class DataMeter<T : BandwidthMeter, S : TransferListener>(
    private val bandwidthMeter: T,
    private val transferListener: S = (bandwidthMeter.transferListener as S?)!!
) : BandwidthMeter, TransferListener {

  override fun getBitrateEstimate(): Long {
    return bandwidthMeter.bitrateEstimate
  }

  override fun getTransferListener(): TransferListener? {
    return bandwidthMeter.transferListener
  }

  override fun addEventListener(eventHandler: Handler?, eventListener: EventListener?) {
    bandwidthMeter.addEventListener(eventHandler, eventListener)
  }

  override fun removeEventListener(eventListener: EventListener?) {
    bandwidthMeter.removeEventListener(eventListener)
  }

  override fun onTransferInitializing(source: DataSource?, dataSpec: DataSpec?,
      isNetwork: Boolean) {
    transferListener.onTransferInitializing(source, dataSpec, isNetwork)
  }

  override fun onTransferStart(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {
    transferListener.onTransferStart(source, dataSpec, isNetwork)
  }

  override fun onTransferEnd(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {
    transferListener.onTransferEnd(source, dataSpec, isNetwork)
  }

  override fun onBytesTransferred(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean,
      bytesTransferred: Int) {
    transferListener.onBytesTransferred(source, dataSpec, isNetwork, bytesTransferred)
  }
}
