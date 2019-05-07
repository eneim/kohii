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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.media.Media
import kohii.v1.BasePlayable
import kohii.v1.Bridge
import kohii.v1.Kohii
import kohii.v1.Playable.Config
import kohii.v1.Playback
import kohii.v1.PlaybackCreator
import kohii.v1.Target
import kohii.v1.ViewTarget

class YouTubePlayable(
  kohii: Kohii,
  media: Media,
  config: Config,
  bridge: Bridge<YouTubePlayerView>,
  playbackCreator: PlaybackCreator<YouTubePlayerView>
) : BasePlayable<YouTubePlayerView>(kohii, media, config, bridge, playbackCreator) {

  override fun <CONTAINER : Any> createBoxedTarget(target: CONTAINER): Target<CONTAINER, YouTubePlayerView> {
    val targetType = target.javaClass
    @Suppress("UNCHECKED_CAST")
    if (ViewGroup::class.java.isAssignableFrom(targetType))
      return ViewTarget<ViewGroup, YouTubePlayerView>(
          target as ViewGroup
      ) as Target<CONTAINER, YouTubePlayerView>
    else throw IllegalArgumentException("Unsupported target type: $targetType")
  }

  override fun onPlayerActive(
    playback: Playback<YouTubePlayerView>,
    player: YouTubePlayerView
  ) {
    bridge.playerView = player
  }

  override fun onPlayerInActive(
    playback: Playback<YouTubePlayerView>,
    player: YouTubePlayerView?
  ) {
    if (bridge.playerView === player) {
      bridge.playerView = null
    }
  }
}
