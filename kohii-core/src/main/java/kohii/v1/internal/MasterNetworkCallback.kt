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

package kohii.v1.internal

import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import kohii.v1.core.Master

@RequiresApi(VERSION_CODES.N)
internal class MasterNetworkCallback(val master: Master) : NetworkCallback() {

  override fun onBlockedStatusChanged(
    network: Network,
    blocked: Boolean
  ) {
    master.onNetworkChanged()
  }

  override fun onCapabilitiesChanged(
    network: Network,
    networkCapabilities: NetworkCapabilities
  ) {
    master.onNetworkChanged()
  }

  override fun onLost(network: Network) {
    master.onNetworkChanged()
  }

  override fun onLinkPropertiesChanged(
    network: Network,
    linkProperties: LinkProperties
  ) {
    master.onNetworkChanged()
  }

  override fun onUnavailable() {
    master.onNetworkChanged()
  }

  override fun onLosing(
    network: Network,
    maxMsToLive: Int
  ) {
    master.onNetworkChanged()
  }

  override fun onAvailable(network: Network) {
    master.onNetworkChanged()
  }
}
