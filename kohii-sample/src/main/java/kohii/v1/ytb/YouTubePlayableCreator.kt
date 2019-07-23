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

package kohii.v1.ytb

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kohii.media.Media
import kohii.v1.Kohii
import kohii.v1.LazyViewPlayback
import kohii.v1.NoCacheRendererPool
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableCreator
import kohii.v1.Playback
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.RendererCreator
import kohii.v1.RendererPool
import kohii.v1.Target

class YouTubePlayableCreator(
  kohii: Kohii
) : PlayableCreator<YouTubePlayerFragment>(kohii, YouTubePlayerFragment::class.java),
    PlaybackCreator<YouTubePlayerFragment> {

  companion object {
    private val youtubePlayerViewCreator =
      object : RendererCreator<YouTubePlayerFragment> {
        override fun <CONTAINER : Any> createRenderer(
          playback: Playback<YouTubePlayerFragment>,
          container: CONTAINER,
          type: Int
        ): YouTubePlayerFragment {
          val fragmentManager =
            when (val provider = playback.manager.provider) {
              is FragmentActivity -> provider.supportFragmentManager
              is Fragment -> provider.childFragmentManager
              else -> throw IllegalArgumentException(
                  "Required Activity or Fragment, found $provider"
              )
            }

          val cache = fragmentManager.findFragmentByTag(playback.tag.toString())
          return cache as? YouTubePlayerFragment ?: YouTubePlayerFragment.newInstance()
        }
      }
  }

  private val rendererPoolCreator: () -> RendererPool<YouTubePlayerFragment> =
    { NoCacheRendererPool(kohii, youtubePlayerViewCreator) }

  override fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Config
  ): Playable<YouTubePlayerFragment> {
    return YouTubePlayable(kohii, media, config, YouTubeBridge(media), this)
  }

  override fun <CONTAINER : Any> createPlayback(
    manager: PlaybackManager,
    target: Target<CONTAINER, YouTubePlayerFragment>,
    playable: Playable<YouTubePlayerFragment>,
    config: Playback.Config
  ): Playback<YouTubePlayerFragment> {
    if (target.container !is ViewGroup) {
      throw IllegalArgumentException("Only accept ViewGroup container")
    }
    val outputHolderPool = manager.fetchRendererPool(YouTubePlayerFragment::class.java)
        ?: rendererPoolCreator().also {
          manager.registerRendererPool(YouTubePlayerFragment::class.java, it)
        }
    @Suppress("UNCHECKED_CAST")
    return LazyViewPlayback(
        kohii,
        playable,
        manager,
        target as Target<ViewGroup, YouTubePlayerFragment>,
        config,
        outputHolderPool
    )
  }
}
