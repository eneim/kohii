/*
 * Copyright (c) 2019 Nam ()Nguyen, nam@ene.im
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

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.media.Media
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableCreator

class YouTubePlayableCreator(
  kohii: Kohii
) : PlayableCreator<YouTubePlayerView>(kohii, YouTubePlayerView::class.java) {

  private val playbackCreator = YouTubePlaybackCreator(kohii)

  override fun createPlayable(
    kohii: Kohii,
    media: Media, // will be a YouTubeMedia
    config: Config
  ): Playable<YouTubePlayerView> {
    @Suppress("UNCHECKED_CAST")
    return YouTubePlayable(
        kohii, media, config,
        YouTubeBridge(media),
        playbackCreator
    )
  }

  override fun cleanUp() {
    // no-op
  }
}
