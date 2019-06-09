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

package kohii.v1.yta

import android.view.ViewGroup
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.media.Media
import kohii.onEachAcquired
import kohii.v1.DefaultRendererPool
import kohii.v1.Kohii
import kohii.v1.LazyViewPlayback
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableCreator
import kohii.v1.Playback
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.RendererCreator
import kohii.v1.RendererPool
import kohii.v1.Target

@Suppress("unused")
class YouTubePlayableCreator(
  kohii: Kohii
) : PlayableCreator<YouTubePlayerView>(kohii, YouTubePlayerView::class.java),
    PlaybackCreator<YouTubePlayerView> {

  companion object {
    private val youtubePlayerViewCreator =
      object : RendererCreator<YouTubePlayerView> {
        override fun <CONTAINER : Any> createRenderer(
          playback: Playback<YouTubePlayerView>,
          container: CONTAINER,
          type: Int
        ): YouTubePlayerView {
          if (container !is ViewGroup) throw IllegalArgumentException("Need ViewGroup container.")
          val iFramePlayerOptions = IFramePlayerOptions.Builder()
              .controls(0)
              .build()

          return YouTubePlayerView(container.context).also {
            it.enableAutomaticInitialization = false
            it.enableBackgroundPlayback(false)
            it.getPlayerUiController()
                .showUi(false)
            it.initialize(object : AbstractYouTubePlayerListener() {}, true, iFramePlayerOptions)
          }
        }
      }
  }

  override fun createPlayable(
    kohii: Kohii,
    media: Media, // will be a YouTubeMedia
    config: Config
  ): Playable<YouTubePlayerView> {
    return YouTubePlayable(kohii, media, config, YouTubeBridge(media), this)
  }

  private val rendererPoolCreator: () -> RendererPool<YouTubePlayerView> = {
    object : DefaultRendererPool<YouTubePlayerView>(kohii, creator = youtubePlayerViewCreator) {
      override fun cleanUp() {
        keyToPool.onEach { it.value.onEachAcquired { player -> player.release() } }
            .clear()
      }
    }
  }

  override fun <CONTAINER : Any> createPlayback(
    manager: PlaybackManager,
    target: Target<CONTAINER, YouTubePlayerView>,
    playable: Playable<YouTubePlayerView>,
    config: Playback.Config
  ): Playback<YouTubePlayerView> {
    if (target.container !is ViewGroup) {
      throw IllegalArgumentException("Only accept ViewGroup container")
    }
    val outputHolderPool =
      manager.fetchRendererPool(YouTubePlayerView::class.java)
          ?: rendererPoolCreator().also {
            manager.registerRendererPool(YouTubePlayerView::class.java, it)
          }
    @Suppress("UNCHECKED_CAST")
    return LazyViewPlayback(
        kohii,
        playable,
        manager,
        target as Target<ViewGroup, YouTubePlayerView>,
        config,
        outputHolderPool
    )
  }

  override fun cleanUp() {
    // no-op
  }
}
