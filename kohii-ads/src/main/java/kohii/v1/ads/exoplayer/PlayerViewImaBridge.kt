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
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.ads.interactivemedia.v3.api.FriendlyObstructionPurpose
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory.AdsLoaderProvider
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.ui.AdOverlayInfo
import com.google.android.exoplayer2.ui.AdOverlayInfo.Purpose
import com.google.android.exoplayer2.ui.AdViewProvider
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ads.AdMedia
import kohii.v1.core.PlayerPool
import kohii.v1.exoplayer.PlayerViewBridge

/**
 * A [PlayerViewBridge] that supports ad media using [ImaAdsLoader].
 */
class PlayerViewImaBridge(
  context: Context,
  media: AdMedia,
  playerPool: PlayerPool<Player>,
  imaBridgeConfig: ImaBridgeConfig,
  private val mediaSourceFactory: DefaultMediaSourceFactory
) : PlayerViewBridge(
  context,
  media,
  playerPool,
  mediaSourceFactory
),
  AdViewProvider,
  AdsLoaderProvider {

  override val mediaItem: MediaItem = MediaItem.Builder()
    .setUri(media.uri)
    .setAdTagUri(media.adTagUri)
    .build()

  // Using Application Context so this View instance can survive configuration changes.
  private val adViewGroup: ViewGroup = FrameLayout(context.applicationContext)
  private val adsLoader: ImaAdsLoader = imaBridgeConfig.adsLoader

  override var renderer: PlayerView?
    get() = super.renderer
    set(value) {
      super.renderer?.let { current ->
        current.adViewGroup.removeView(adViewGroup)
        adsLoader.adDisplayContainer?.unregisterAllFriendlyObstructions()
      }
      super.renderer = value
      if (value != null) {
        value.adViewGroup.addView(adViewGroup)
        val adDisplayContainer = adsLoader.adDisplayContainer ?: return
        for (adOverlayInfo in value.adOverlayInfos) {
          adDisplayContainer.registerFriendlyObstruction(
            ImaSdkFactory.getInstance().createFriendlyObstruction(
              adOverlayInfo.view,
              getFriendlyObstructionPurpose(adOverlayInfo.purpose),
              adOverlayInfo.reasonDetail
            )
          )
        }
      }
    }

  override fun prepare(loadSource: Boolean) {
    if (loadSource) {
      mediaSourceFactory.setAdViewProvider(this)
      mediaSourceFactory.setAdsLoaderProvider(this)
    }
    super.prepare(loadSource)
  }

  override fun ready() {
    // Call before super.ready() so the Ads setup is injected into the MediaSource.
    mediaSourceFactory.setAdViewProvider(this)
    mediaSourceFactory.setAdsLoaderProvider(this)
    super.ready()
    adsLoader.setPlayer(player)
  }

  override fun reset(resetPlayer: Boolean) {
    mediaSourceFactory.setAdViewProvider(null)
    mediaSourceFactory.setAdsLoaderProvider(null)
    super.reset(resetPlayer)
    adsLoader.setPlayer(null)
  }

  override fun release() {
    super.release()
    mediaSourceFactory.setAdViewProvider(null)
    mediaSourceFactory.setAdsLoaderProvider(null)
    adsLoader.setPlayer(null)
    adsLoader.release()
  }

  //region AdsLoaderProvider implementation
  override fun getAdsLoader(adsConfiguration: AdsConfiguration): AdsLoader = adsLoader
  //endregion

  //region AdsLoader.AdViewProvider implementation
  override fun getAdViewGroup(): ViewGroup = adViewGroup

  // This bridge will manually register the AdOverlayInfo
  override fun getAdOverlayInfos(): List<AdOverlayInfo> = emptyList()
  //endregion

  private companion object {
    fun getFriendlyObstructionPurpose(@Purpose purpose: Int): FriendlyObstructionPurpose {
      return when (purpose) {
        AdOverlayInfo.PURPOSE_CONTROLS -> FriendlyObstructionPurpose.VIDEO_CONTROLS
        AdOverlayInfo.PURPOSE_CLOSE_AD -> FriendlyObstructionPurpose.CLOSE_AD
        AdOverlayInfo.PURPOSE_NOT_VISIBLE -> FriendlyObstructionPurpose.NOT_VISIBLE
        AdOverlayInfo.PURPOSE_OTHER -> FriendlyObstructionPurpose.OTHER
        else -> FriendlyObstructionPurpose.OTHER
      }
    }
  }
}
