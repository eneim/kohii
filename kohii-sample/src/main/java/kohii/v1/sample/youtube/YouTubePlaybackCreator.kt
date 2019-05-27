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

package kohii.v1.sample.youtube

import android.view.ViewGroup
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.forEach
import kohii.onEachAcquired
import kohii.v1.Kohii
import kohii.v1.LazyViewPlayback
import kohii.v1.OutputHolderCreator
import kohii.v1.OutputHolderPool
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.Playback.Config
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.Target

class YouTubePlaybackCreator(val kohii: Kohii) : PlaybackCreator<YouTubePlayerView> {

  private val youtubePlayerViewCreator =
    object : OutputHolderCreator<ViewGroup, YouTubePlayerView> {
      override fun createOutputHolder(
        container: ViewGroup,
        type: Int
      ): YouTubePlayerView {
        return YouTubePlayerView(container.context).also {
          it.enableAutomaticInitialization = false
          it.enableBackgroundPlayback(false)
          it.getPlayerUiController()
              .showUi(false)
          it.initialize(object : AbstractYouTubePlayerListener() {}, true)
        }
      }
    }

  private val outputHolderPoolCreator: () -> OutputHolderPool<ViewGroup, YouTubePlayerView> = {
    object : OutputHolderPool<ViewGroup, YouTubePlayerView>(2, youtubePlayerViewCreator) {
      override fun cleanUp() {
        this.pools.forEach { pool, _ -> pool.onEachAcquired { it.release() } }
      }
    }
  }

  override fun <CONTAINER : Any> createPlayback(
    manager: PlaybackManager,
    target: Target<CONTAINER, YouTubePlayerView>,
    playable: Playable<YouTubePlayerView>,
    config: Config
  ): Playback<YouTubePlayerView> {
    if (target.requireContainer() !is ViewGroup) {
      throw IllegalArgumentException("Only accept ViewGroup container")
    }
    val key = ViewGroup::class.java to YouTubePlayerView::class.java
    val outputHolderPool =
      manager.fetchOutputHolderPool(key)
          ?: outputHolderPoolCreator().also { manager.registerOutputHolderPool(key, it) }
    @Suppress("UNCHECKED_CAST")
    return LazyViewPlayback(
        kohii, playable, manager, target as Target<ViewGroup, YouTubePlayerView>, config,
        outputHolderPool
    )
  }
}
