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

package kohii.v1.exoplayer

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import kohii.v1.BuildConfig
import kohii.v1.core.Common
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.PlayableCreator
import kohii.v1.core.Playback
import kohii.v1.core.PlayerPool
import kohii.v1.core.RendererProviderFactory
import kohii.v1.exoplayer.ExoPlayerCache.lruCacheSingleton
import kohii.v1.exoplayer.Kohii.Builder
import kohii.v1.media.Media
import kohii.v1.utils.Capsule

open class Kohii constructor(
  master: Master,
  playableCreator: PlayableCreator<PlayerView> = PlayerViewPlayableCreator(master),
  private val rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
) : Engine<PlayerView>(master, playableCreator) {

  private constructor(context: Context) : this(Master[context])

  companion object {

    private val capsule = Capsule(::Kohii)

    @JvmStatic // convenient static call for Java
    operator fun get(context: Context) = capsule.get(context)

    @JvmStatic // convenient static call for Java
    operator fun get(fragment: Fragment) = capsule.get(fragment.requireContext())
  }

  // Adapt from ExoPlayer demo app.
  /* init {
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    if (CookieHandler.getDefault() !== cookieManager) {
      CookieHandler.setDefault(cookieManager)
    }
  } */

  override fun prepare(manager: Manager) {
    manager.registerRendererProvider(PlayerView::class.java, rendererProviderFactory())
  }

  /**
   * Creates a [ControlDispatcher] that can be used to setup the renderer when it is a [PlayerView].
   * This method must be used for the [Playback] that supports manual playback control (the
   * [Playback.Config.controller] is not null).
   */
  fun createControlDispatcher(playback: Playback): ControlDispatcher {
    requireNotNull(playback.config.controller) {
      "Playback needs to be setup with a Controller to use this method."
    }

    return DefaultControlDispatcher(playback)
  }

  class Builder(context: Context) {

    private val master = Master[context.applicationContext]

    private var playableCreator: PlayableCreator<PlayerView> =
      PlayerViewPlayableCreator(master)

    private var rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }

    fun setPlayableCreator(playableCreator: PlayableCreator<PlayerView>): Builder = apply {
      this.playableCreator = playableCreator
    }

    fun setRendererProviderFactory(factory: RendererProviderFactory): Builder = apply {
      this.rendererProviderFactory = factory
    }

    fun build(): Kohii = Kohii(
        master = master,
        playableCreator = playableCreator,
        rendererProviderFactory = rendererProviderFactory
    ).also {
      master.registerEngine(it)
    }
  }
}

/**
 * Creates a new [Kohii] instance using an [ExoPlayerConfig]. Note that an application should not
 * hold many instance of [Kohii].
 *
 * @param context the [Context].
 * @param config the [ExoPlayerConfig].
 */
fun createKohii(context: Context, config: ExoPlayerConfig): Kohii {
  val bridgeCreatorFactory: PlayerViewBridgeCreatorFactory = { appContext ->
    val userAgent = Common.getUserAgent(appContext, BuildConfig.LIB_NAME)
    val httpDataSource = DefaultHttpDataSourceFactory(userAgent)

    val playerPool = ExoPlayerPool(
        context = appContext,
        clock = config.clock,
        bandwidthMeterFactory = config,
        trackSelectorFactory = config,
        loadControlFactory = config,
        renderersFactory = DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(config.enableDecoderFallback)
            .setAllowedVideoJoiningTimeMs(config.allowedVideoJoiningTimeMs)
            .setExtensionRendererMode(config.extensionRendererMode)
            .setMediaCodecSelector(config.mediaCodecSelector)
            .setPlayClearSamplesWithoutKeys(config.playClearSamplesWithoutKeys)
    )
    val mediaCache: Cache = config.cache ?: lruCacheSingleton.get(context)
    val drmSessionManagerProvider =
      config.drmSessionManagerProvider ?: DefaultDrmSessionManagerProvider(
          appContext, httpDataSource
      )
    val upstreamFactory = DefaultDataSourceFactory(appContext, httpDataSource)
    val mediaSourceFactoryProvider = DefaultMediaSourceFactoryProvider(
        upstreamFactory, drmSessionManagerProvider, mediaCache
    )
    PlayerViewBridgeCreator(playerPool, mediaSourceFactoryProvider)
  }

  val playableCreator = PlayerViewPlayableCreator.Builder(context.applicationContext)
      .setBridgeCreatorFactory(bridgeCreatorFactory)
      .build()

  return Builder(context).setPlayableCreator(playableCreator).build()
}

/**
 * Creates a new [Kohii] instance using a custom [playerCreator], [mediaSourceFactoryCreator] and
 * [rendererProviderFactory]. Note that an application should not hold many instance of [Kohii].
 *
 * @param context the [Context].
 * @param playerCreator the custom creator for the [Player]. If `null`, it will use the default one.
 * @param mediaSourceFactoryCreator the custom creator for the [MediaSourceFactory]. If `null`, it
 * will use the default one.
 * @param rendererProviderFactory the custom [RendererProviderFactory].
 */
@JvmOverloads
fun createKohii(
  context: Context,
  playerCreator: ((Context) -> Player)? = null,
  mediaSourceFactoryCreator: ((Media) -> MediaSourceFactory)? = null,
  rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
): Kohii {
  val playerPool = if (playerCreator == null) {
    ExoPlayerPool(context = context)
  } else {
    object : PlayerPool<Player>() {
      override fun recyclePlayerForMedia(media: Media): Boolean = false
      override fun createPlayer(media: Media): Player = playerCreator(context)
      override fun destroyPlayer(player: Player) = player.release()
    }
  }

  val mediaSourceFactoryProvider = if (mediaSourceFactoryCreator == null) {
    DefaultMediaSourceFactoryProvider(context)
  } else {
    object : MediaSourceFactoryProvider {
      override fun provideMediaSourceFactory(media: Media): MediaSourceFactory =
        mediaSourceFactoryCreator(media)
    }
  }

  return Builder(context)
      .setPlayableCreator(
          PlayerViewPlayableCreator.Builder(context)
              .setBridgeCreatorFactory {
                PlayerViewBridgeCreator(playerPool, mediaSourceFactoryProvider)
              }
              .build()
      )
      .setRendererProviderFactory(rendererProviderFactory)
      .build()
}
