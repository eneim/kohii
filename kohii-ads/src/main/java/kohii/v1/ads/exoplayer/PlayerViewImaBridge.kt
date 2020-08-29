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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ads.AdMedia
import kohii.v1.core.PlayerPool
import kohii.v1.exoplayer.MediaSourceFactoryProvider
import kohii.v1.exoplayer.PlayerViewBridge

/**
 * Configuration for a [PlayerViewImaBridge].
 */
data class ImaBridgeConfig(
  val adsLoader: ImaAdsLoader,
  val adsMediaSourceFactory: MediaSourceFactory
)

class PlayerViewImaBridge(
  context: Context,
  media: AdMedia,
  playerPool: PlayerPool<Player>,
  mediaSourceFactoryProvider: MediaSourceFactoryProvider,
  imaBridgeConfig: ImaBridgeConfig
) : PlayerViewBridge(context, media, playerPool, mediaSourceFactoryProvider),
    AdsLoader.AdViewProvider {

  private val adsLoader: ImaAdsLoader = imaBridgeConfig.adsLoader
  private val adsMediaSourceFactory: MediaSourceFactory = imaBridgeConfig.adsMediaSourceFactory
  private val mediaSourceFactory: MediaSourceFactory =
    mediaSourceFactoryProvider.provideMediaSourceFactory(media)

  // Using Application Context so this View can survive configuration changes.
  private val adViewGroup: ViewGroup = FrameLayout(context.applicationContext)
  private val adDisplayContainer: AdDisplayContainer = adsLoader.adDisplayContainer

  override var renderer: PlayerView?
    get() = super.renderer
    set(value) {
      super.renderer?.let { current ->
        current.adViewGroup.removeView(adViewGroup)
        @Suppress("DEPRECATION")
        adDisplayContainer.unregisterAllVideoControlsOverlays()
        adDisplayContainer.unregisterAllFriendlyObstructions()
      }
      super.renderer = value
      if (value != null) {
        value.adViewGroup.addView(adViewGroup)
        value.adOverlayViews.forEach {
          @Suppress("DEPRECATION")
          adDisplayContainer.registerVideoControlsOverlay(it)
        }
      }
    }

  override fun createMediaSource(): MediaSource = AdsMediaSource(
      mediaSourceFactory.createMediaSource(media.uri),
      adsMediaSourceFactory,
      adsLoader,
      this
  )

  override fun ready() {
    super.ready()
    adsLoader.setPlayer(player)
  }

  override fun reset(resetPlayer: Boolean) {
    super.reset(resetPlayer)
    adsLoader.setPlayer(null)
  }

  override fun release() {
    super.release()
    adsLoader.setPlayer(null)
    adsLoader.release()
  }

  override fun getAdViewGroup(): ViewGroup = adViewGroup

  override fun getAdOverlayViews(): Array<View> = arrayOf()
}
