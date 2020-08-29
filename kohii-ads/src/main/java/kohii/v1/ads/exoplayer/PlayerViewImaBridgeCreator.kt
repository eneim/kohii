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

package kohii.v1.ads.exoplayer

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ads.AdMedia
import kohii.v1.ads.Manilo
import kohii.v1.core.Bridge
import kohii.v1.core.BridgeCreator
import kohii.v1.core.PlayerPool
import kohii.v1.exoplayer.MediaSourceFactoryProvider
import kohii.v1.media.Media

class PlayerViewImaBridgeCreator(
  private val playerPool: PlayerPool<Player>,
  private val mediaSourceFactoryProvider: MediaSourceFactoryProvider,
  private val adsMediaSourceFactory: MediaSourceFactory,
  private val imaAdsLoaderBuilder: ImaAdsLoader.Builder? = null
) : BridgeCreator<PlayerView> {

  override fun createBridge(context: Context, media: Media): Bridge<PlayerView> {
    require(media is AdMedia)
    val adsLoaderBuilder = this.imaAdsLoaderBuilder ?: ImaAdsLoader.Builder(context)
        .setAdEventListener(Manilo[context])
    val adsLoader = adsLoaderBuilder.buildForAdTag(media.adTagUri)
    return PlayerViewImaBridge(
        context,
        media,
        playerPool,
        mediaSourceFactoryProvider,
        ImaBridgeConfig(adsLoader, adsMediaSourceFactory)
    )
  }

  override fun cleanUp(): Unit = playerPool.clear()
}
