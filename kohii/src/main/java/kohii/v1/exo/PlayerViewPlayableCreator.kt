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

package kohii.v1.exo

import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.BridgeProvider
import kohii.v1.Kohii
import kohii.v1.LazyViewPlayback
import kohii.v1.OutputHolderCreator
import kohii.v1.OutputHolderPool
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableCreator
import kohii.v1.Playback
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.Target
import kohii.v1.ViewPlayback

/**
 * @author eneim (2019/03/14)
 *
 * Need to give the Config more power. Because Playable lives at global scope, so is instance of this class.
 * Responsibilities:
 * - Allow binding to specific type of Container.
 * - Judge from the Container type to build correct Playable.
 */
class PlayerViewPlayableCreator(
  kohii: Kohii,
  private val outputHolderCreator: OutputHolderCreator<ViewGroup, PlayerView> = PlayerViewCreator(),
  bridgeCreatorFactory: (Kohii) -> BridgeProvider<PlayerView> = { kohii.defaultBridgeProvider }
) : PlayableCreator<PlayerView>(kohii, PlayerView::class.java),
    PlaybackCreator<PlayerView> {

  private val bridgeProvider = bridgeCreatorFactory.invoke(kohii)

  override fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Config
  ): Playable<PlayerView> {
    return PlayerViewPlayable(kohii, media, config, bridgeProvider, this)
  }

  override fun <CONTAINER : Any> createPlayback(
    manager: PlaybackManager,
    target: Target<CONTAINER, PlayerView>,
    playable: Playable<PlayerView>,
    config: Playback.Config
  ): Playback<PlayerView> {
    return when (val container = target.requireContainer()) {
      is PlayerView -> {
        ViewPlayback(
            kohii,
            playable,
            manager,
            container,
            config
        )
      }

      is ViewGroup -> {
        val key = ViewGroup::class.java to PlayerView::class.java
        val outputHolderPool =
          manager.fetchOutputHolderPool(key)
              ?: OutputHolderPool(2, outputHolderCreator).also {
                manager.registerOutputHolderPool(key, it)
              }

        @Suppress("UNCHECKED_CAST")
        LazyViewPlayback(
            kohii,
            playable,
            manager,
            boxedTarget = target as Target<ViewGroup, PlayerView>,
            options = config,
            outputHolderPool = outputHolderPool
        )
      }
      else -> throw IllegalArgumentException("Unsupported container type: ${container::javaClass}")
    }
  }

  override fun cleanUp() {
    bridgeProvider.cleanUp()
  }
}
