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
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v1.core.BridgeCreator
import kohii.v1.core.Common
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playable.Config
import kohii.v1.core.PlayableCreator
import kohii.v1.media.Media
import kotlin.LazyThreadSafetyMode.NONE

typealias StyledPlayerViewBridgeCreatorFactory = (Context) -> BridgeCreator<StyledPlayerView>

class StyledPlayerViewPlayableCreator internal constructor(
  private val master: Master,
  private val bridgeCreatorFactory: StyledPlayerViewBridgeCreatorFactory =
    defaultBridgeCreatorFactory
) : PlayableCreator<StyledPlayerView>(StyledPlayerView::class.java) {

  constructor(context: Context) : this(Master[context.applicationContext])

  companion object {

    // Only pass Application to this method.
    private val defaultBridgeCreatorFactory: StyledPlayerViewBridgeCreatorFactory = { context ->
      // ExoPlayerProvider
      val playerPool = ExoPlayerPool(
        context = context,
        userAgent = Common.getUserAgent(context, BuildConfig.LIB_NAME)
      )
      StyledPlayerViewBridgeCreator(playerPool, playerPool.defaultMediaSourceFactory)
    }
  }

  private val bridgeCreator: Lazy<BridgeCreator<StyledPlayerView>> = lazy(NONE) {
    bridgeCreatorFactory(master.app)
  }

  override fun createPlayable(
    config: Config,
    media: Media
  ): Playable {
    return StyledPlayerViewPlayable(
      master,
      media,
      config,
      bridgeCreator.value.createBridge(master.app, media)
    )
  }

  override fun cleanUp() {
    if (bridgeCreator.isInitialized()) bridgeCreator.value.cleanUp()
  }

  class Builder(context: Context) {

    private val app = context.applicationContext

    private var bridgeCreatorFactory: StyledPlayerViewBridgeCreatorFactory =
      defaultBridgeCreatorFactory

    fun setBridgeCreatorFactory(factory: StyledPlayerViewBridgeCreatorFactory): Builder = apply {
      this.bridgeCreatorFactory = factory
    }

    fun build(): PlayableCreator<StyledPlayerView> = StyledPlayerViewPlayableCreator(
      Master[app],
      bridgeCreatorFactory
    )
  }
}
