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
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ads.AdMedia
import kohii.v1.ads.Manilo
import kohii.v1.core.Bridge
import kohii.v1.core.BridgeCreator
import kohii.v1.core.PlayerPool
import kohii.v1.exoplayer.MediaSourceFactoryProvider
import kohii.v1.exoplayer.PlayerViewBridge
import kohii.v1.media.Media

/**
 * A [BridgeCreator] that creates instance of [PlayerViewImaBridge] if the [Media] contains an ad
 * media Uri, or fallback to a [PlayerViewBridge] otherwise.
 *
 * @param playerPool The [PlayerPool] for [Player] instances.
 * @param mediaSourceFactoryProvider The [MediaSourceFactoryProvider] for the video of the [Media].
 * @param adsMediaSourceFactory The [MediaSourceFactory] to create [MediaSource] for the Ad.
 * @param imaAdsLoaderBuilder The [ImaAdsLoader.Builder] to create the [ImaAdsLoader]. When null,
 * the library will use a default one. When the library uses a default builder, it also sets a
 * default [AdEventListener] for debugging purpose. Applications that want to use their own
 * listeners should provide their own Builders.
 */
class PlayerViewImaBridgeCreator(
  private val playerPool: PlayerPool<Player>,
  private val mediaSourceFactoryProvider: MediaSourceFactoryProvider,
  private val adsMediaSourceFactory: MediaSourceFactory,
  private val imaAdsLoaderBuilder: ImaAdsLoader.Builder? = null
) : BridgeCreator<PlayerView> {

  override fun createBridge(context: Context, media: Media): Bridge<PlayerView> {
    val adTagUri = (media as? AdMedia)?.adTagUri
    return if (adTagUri != null) {
      val adsLoaderBuilder = imaAdsLoaderBuilder ?: ImaAdsLoader.Builder(context)
          .setAdEventListener(Manilo[context])
      val adsLoader = adsLoaderBuilder.buildForAdTag(adTagUri)
      PlayerViewImaBridge(
          context,
          media,
          playerPool,
          mediaSourceFactoryProvider,
          ImaBridgeConfig(adsLoader, adsMediaSourceFactory)
      )
    } else {
      PlayerViewBridge(
          context,
          media,
          playerPool,
          mediaSourceFactoryProvider
      )
    }
  }

  override fun cleanUp(): Unit = playerPool.clear()
}
