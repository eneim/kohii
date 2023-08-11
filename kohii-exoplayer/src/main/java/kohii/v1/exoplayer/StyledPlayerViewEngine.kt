/*
 * Copyright (c) 2023 Nam Nguyen, nam@ene.im
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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v1.core.Common
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.PlayableCreator
import kohii.v1.core.PlayerPool
import kohii.v1.core.RendererProviderFactory
import kohii.v1.media.Media
import kohii.v1.utils.Capsule

open class StyledPlayerViewEngine constructor(
  master: Master,
  playableCreator: PlayableCreator<StyledPlayerView> = StyledPlayerViewPlayableCreator(master),
  private val rendererProviderFactory: RendererProviderFactory = ::StyledPlayerViewProvider
) : Engine<StyledPlayerView>(master, playableCreator) {

  private constructor(context: Context) : this(Master[context])

  companion object {

    private val capsule = Capsule(::StyledPlayerViewEngine)

    @JvmStatic // convenient static call for Java
    operator fun get(context: Context) = capsule.get(context)

    @JvmStatic // convenient static call for Java
    operator fun get(fragment: Fragment) = capsule.get(fragment.requireContext())
  }

  override fun prepare(manager: Manager) {
    manager.registerRendererProvider(StyledPlayerView::class.java, rendererProviderFactory())
  }

  // TODO: replace with custom ForwardingPlayer.
  /* fun createControlDispatcher(playback: Playback): ControlDispatcher {
    requireNotNull(playback.config.controller) {
      "Playback needs to be setup with a Controller to use this method."
    }

    return DefaultControlDispatcher(playback)
  } */

  class Builder(context: Context) {

    private val master = Master[context.applicationContext]

    private var playableCreator: PlayableCreator<StyledPlayerView> =
      StyledPlayerViewPlayableCreator(master)

    private var rendererProviderFactory: RendererProviderFactory = { StyledPlayerViewProvider() }

    fun setPlayableCreator(playableCreator: PlayableCreator<StyledPlayerView>): Builder = apply {
      this.playableCreator = playableCreator
    }

    fun setRendererProviderFactory(factory: RendererProviderFactory): Builder = apply {
      this.rendererProviderFactory = factory
    }

    fun build(): StyledPlayerViewEngine = StyledPlayerViewEngine(
      master = master,
      playableCreator = playableCreator,
      rendererProviderFactory = rendererProviderFactory
    ).also {
      master.registerEngine(it)
    }
  }
}

/**
 * Creates a new [StyledPlayerViewEngine] instance using an [ExoPlayerConfig]. Note that an
 * application should not hold many instance of [StyledPlayerViewEngine].
 *
 * @param context the [Context].
 * @param config the [ExoPlayerConfig].
 */
fun createStyledPlayerViewEngine(
  context: Context,
  config: ExoPlayerConfig
): StyledPlayerViewEngine {
  val bridgeCreatorFactory: StyledPlayerViewBridgeCreatorFactory = { appContext ->
    val userAgent = Common.getUserAgent(appContext, BuildConfig.LIB_NAME)
    val playerPool = config.createDefaultPlayerPool(
      context = context,
      userAgent = userAgent
    )
    StyledPlayerViewBridgeCreator(
      playerPool = playerPool,
      mediaSourceFactory = playerPool.defaultMediaSourceFactory
    )
  }

  val playableCreator = StyledPlayerViewPlayableCreator.Builder(context.applicationContext)
    .setBridgeCreatorFactory(bridgeCreatorFactory)
    .build()

  return StyledPlayerViewEngine.Builder(context).setPlayableCreator(playableCreator).build()
}

/**
 * Creates a new [StyledPlayerViewEngine] instance using a custom [playerCreator] and
 * [rendererProviderFactory].
 *
 * Note that an application should not hold many instance of [StyledPlayerViewEngine].
 *
 * @param context the [Context].
 * @param playerCreator the custom creator for the [Player]. If `null`, it will use the default one.
 * @param rendererProviderFactory the custom [RendererProviderFactory].
 */
@JvmOverloads
fun createStyledPlayerViewEngine(
  context: Context,
  playerCreator: ((Context) -> Player)? = null,
  rendererProviderFactory: RendererProviderFactory = { StyledPlayerViewProvider() }
): StyledPlayerViewEngine {
  val playerPool = if (playerCreator == null) {
    ExoPlayerPool(
      context = context.applicationContext,
      userAgent = Common.getUserAgent(context.applicationContext, BuildConfig.LIB_NAME)
    )
  } else {
    object : PlayerPool<Player>() {
      override fun recyclePlayerForMedia(media: Media): Boolean = false
      override fun createPlayer(media: Media): Player = playerCreator(context)
      override fun destroyPlayer(player: Player) = player.release()
    }
  }

  val mediaSourceFactory = (playerPool as? ExoPlayerPool)?.defaultMediaSourceFactory
    ?: DefaultMediaSourceFactory(context.applicationContext)

  return StyledPlayerViewEngine.Builder(context)
    .setPlayableCreator(
      StyledPlayerViewPlayableCreator.Builder(context)
        .setBridgeCreatorFactory {
          StyledPlayerViewBridgeCreator(playerPool, mediaSourceFactory)
        }
        .build()
    )
    .setRendererProviderFactory(rendererProviderFactory)
    .build()
}
