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

package kohii.v1.experiments.internal

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playable.Config
import kohii.v1.core.PlayableCreator
import kohii.v1.media.Media

internal class UnofficialYouTubePlayerPlayableCreator(
  private val master: Master
) : PlayableCreator<YouTubePlayerView>(YouTubePlayerView::class.java) {

  override fun createPlayable(
    config: Config,
    media: Media
  ): Playable {
    return UnofficialYouTubePlayerPlayable(
      master,
      media,
      config,
      UnofficialYouTubePlayerBridge(media)
    )
  }

  override fun cleanUp() {
    // nothing to do
  }
}
