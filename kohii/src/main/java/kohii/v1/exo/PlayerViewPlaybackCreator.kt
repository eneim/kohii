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
import kohii.v1.DefaultRendererPool
import kohii.v1.Kohii
import kohii.v1.LazyViewPlayback
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.Playback.Config
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.RendererCreator
import kohii.v1.Target
import kohii.v1.ViewPlayback

class PlayerViewPlaybackCreator(
  private val kohii: Kohii,
  private val rendererCreator: RendererCreator<PlayerView> = PlayerViewCreator()
) : PlaybackCreator<PlayerView> {

  override fun <CONTAINER : Any> createPlayback(
    manager: PlaybackManager,
    target: Target<CONTAINER, PlayerView>,
    playable: Playable<PlayerView>,
    config: Config
  ): Playback<PlayerView> {
    return when (val container = target.container) {
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
        val rendererPool =
          manager.fetchRendererPool(PlayerView::class.java)
              ?: DefaultRendererPool(kohii, creator = rendererCreator).also {
                manager.registerRendererPool(PlayerView::class.java, it)
              }

        @Suppress("UNCHECKED_CAST")
        (LazyViewPlayback(
            kohii,
            playable,
            manager,
            boxedTarget = target as Target<ViewGroup, PlayerView>,
            options = config,
            rendererPool = rendererPool
        ))
      }
      else -> throw IllegalArgumentException("Unsupported container type: ${container::javaClass}")
    }
  }
}
