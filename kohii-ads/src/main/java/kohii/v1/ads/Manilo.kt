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

package kohii.v1.ads

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import kohii.v1.ads.exoplayer.PlayerViewImaBridgeCreator
import kohii.v1.core.Common
import kohii.v1.core.Engine
import kohii.v1.core.Master
import kohii.v1.core.PlayableCreator
import kohii.v1.core.PlayerPool
import kohii.v1.core.RendererProviderFactory
import kohii.v1.exoplayer.DefaultDrmSessionManagerProvider
import kohii.v1.exoplayer.DefaultMediaSourceFactoryProvider
import kohii.v1.exoplayer.ExoPlayerCache
import kohii.v1.exoplayer.ExoPlayerConfig
import kohii.v1.exoplayer.ExoPlayerPool
import kohii.v1.exoplayer.Kohii
import kohii.v1.exoplayer.MediaSourceFactoryProvider
import kohii.v1.exoplayer.PlayerViewBridgeCreatorFactory
import kohii.v1.exoplayer.PlayerViewPlayableCreator
import kohii.v1.exoplayer.PlayerViewProvider
import kohii.v1.exoplayer.createDefaultMediaSourceFactoryProvider
import kohii.v1.exoplayer.createDefaultPlayerPool
import kohii.v1.logInfo
import kohii.v1.media.Media
import kohii.v1.utils.Capsule

/**
 * An [Engine] that employs the ExoPlayer API and supports Ads using the Google IMA SDK (via the
 * ExoPlayer Ima extension).
 */
class Manilo(
  master: Master,
  playableCreator: PlayableCreator<PlayerView> = PlayerViewPlayableCreator.Builder(master.app)
      .setBridgeCreatorFactory(defaultBridgeCreatorFactory)
      .build(),
  rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
) : Kohii(master, playableCreator, rendererProviderFactory), AdEventListener {

  private constructor(context: Context) : this(Master[context])

  /**
   * Creates a new instance of [Manilo] from a [Context], a custom [MediaSourceFactory] and a custom
   * [ImaAdsLoader.Builder]. Application can also use a custom [PlayerPool] for the [Player] and a
   * custom [MediaSourceFactoryProvider], but they are all optional.
   */
  constructor(
    context: Context,
    playerPool: PlayerPool<Player> = ExoPlayerPool(context = context),
    mediaSourceFactoryProvider: MediaSourceFactoryProvider = with(context.applicationContext) {
      val userAgent = Common.getUserAgent(this, BuildConfig.LIB_NAME)
      val httpDataSourceFactory = DefaultHttpDataSourceFactory(userAgent)

      val mediaCache: Cache = ExoPlayerCache.lruCacheSingleton.get(this)
      val upstreamFactory = DefaultDataSourceFactory(this, httpDataSourceFactory)
      val drmSessionManagerProvider = DefaultDrmSessionManagerProvider(this, httpDataSourceFactory)

      DefaultMediaSourceFactoryProvider(upstreamFactory, drmSessionManagerProvider, mediaCache)
    },
    adsMediaSourceFactory: MediaSourceFactory,
    imaAdsLoaderBuilder: ImaAdsLoader.Builder?,
    rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
  ) : this(
      master = Master[context],
      playableCreator = PlayerViewPlayableCreator.Builder(context.applicationContext)
          .setBridgeCreatorFactory {
            PlayerViewImaBridgeCreator(
                playerPool,
                mediaSourceFactoryProvider = mediaSourceFactoryProvider,
                adsMediaSourceFactory = adsMediaSourceFactory,
                imaAdsLoaderBuilder = imaAdsLoaderBuilder
            )
          }
          .build(),
      rendererProviderFactory = rendererProviderFactory
  )

  /**
   * Creates a new instance of [Manilo] from a [Context], an [ExoPlayerConfig], a custom
   * [MediaSourceFactory] and a custom [ImaAdsLoader.Builder].
   */
  constructor(
    context: Context,
    config: ExoPlayerConfig,
    adsMediaSourceFactory: MediaSourceFactory,
    imaAdsLoaderBuilder: ImaAdsLoader.Builder?,
    rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
  ) : this(
      context = context,
      playerPool = config.createDefaultPlayerPool(context),
      mediaSourceFactoryProvider = kotlin.run {
        val userAgent = Common.getUserAgent(context.applicationContext, BuildConfig.LIB_NAME)
        val dataSourceFactory = DefaultHttpDataSourceFactory(userAgent)
        config.createDefaultMediaSourceFactoryProvider(context, dataSourceFactory)
      },
      adsMediaSourceFactory = adsMediaSourceFactory,
      imaAdsLoaderBuilder = imaAdsLoaderBuilder,
      rendererProviderFactory = rendererProviderFactory
  )

  /**
   * Creates a new instance of [Manilo] from a [Context], a custom way to create [Player] instance,
   * a custom way to create [MediaSourceFactory] for the [Media] item, a custom [MediaSourceFactory]
   * for the ad media and a custom [ImaAdsLoader.Builder].
   *
   * @param playerCreator Creates a new [Player] instance.
   */
  constructor(
    context: Context,
    playerCreator: ((Context) -> Player)? = null,
    mediaSourceFactoryCreator: ((Media) -> MediaSourceFactory)? = null,
    adsMediaSourceFactory: MediaSourceFactory,
    imaAdsLoaderBuilder: ImaAdsLoader.Builder?,
    rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
  ) : this(
      context = context.applicationContext,
      playerPool = if (playerCreator == null) {
        ExoPlayerPool(context = context.applicationContext)
      } else {
        object : PlayerPool<Player>() {
          override fun recyclePlayerForMedia(media: Media): Boolean = false
          override fun createPlayer(media: Media): Player =
            playerCreator(context.applicationContext)

          override fun destroyPlayer(player: Player) = player.release()
        }
      },
      mediaSourceFactoryProvider = if (mediaSourceFactoryCreator == null) {
        DefaultMediaSourceFactoryProvider(context.applicationContext)
      } else {
        object : MediaSourceFactoryProvider {
          override fun provideMediaSourceFactory(media: Media): MediaSourceFactory =
            mediaSourceFactoryCreator(media)
        }
      },
      adsMediaSourceFactory = adsMediaSourceFactory,
      imaAdsLoaderBuilder = imaAdsLoaderBuilder,
      rendererProviderFactory = rendererProviderFactory
  )

  companion object {

    private val capsule = Capsule(::Manilo)

    operator fun get(context: Context): Manilo = capsule.get(context)

    operator fun get(fragment: Fragment): Manilo = capsule.get(fragment.requireContext())

    // Only pass Application to this method.
    private val defaultBridgeCreatorFactory: PlayerViewBridgeCreatorFactory = { context ->
      val userAgent = Common.getUserAgent(context, BuildConfig.LIB_NAME)
      val httpDataSourceFactory = DefaultHttpDataSourceFactory(userAgent)

      // ExoPlayerProvider
      val playerPool: PlayerPool<Player> = ExoPlayerPool(context = context)

      // MediaSourceFactoryProvider
      val mediaCache: Cache = ExoPlayerCache.lruCacheSingleton.get(context)
      val upstreamFactory = DefaultDataSourceFactory(context, httpDataSourceFactory)
      val drmSessionManagerProvider =
        DefaultDrmSessionManagerProvider(context, httpDataSourceFactory)
      val mediaSourceFactoryProvider: MediaSourceFactoryProvider =
        DefaultMediaSourceFactoryProvider(upstreamFactory, drmSessionManagerProvider, mediaCache)

      // BridgeCreator
      PlayerViewImaBridgeCreator(
          playerPool = playerPool,
          mediaSourceFactoryProvider = mediaSourceFactoryProvider,
          adsMediaSourceFactory = ProgressiveMediaSource.Factory(httpDataSourceFactory)
      )
    }
  }

  // AdEventListener

  // This callback only works when [Manilo] uses a default [ImaAdsLoader.Builder].
  override fun onAdEvent(adEvent: AdEvent) {
    "AdEvent: $adEvent".logInfo()
  }
}
