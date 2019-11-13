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

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.core.Master
import kohii.core.Playable
import kohii.core.Playable.Config
import kohii.core.PlayableCreator
import kohii.core.Rebinder
import kohii.media.Media
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
class YouTubeARebinder(
  override val tag: @RawValue Any
) : Rebinder<YouTubePlayerView>(tag, YouTubePlayerView::class.java)

class NewYtPlayableCreator : PlayableCreator<YouTubePlayerView>(YouTubePlayerView::class.java) {

  override fun createPlayable(
    master: Master,
    config: Config,
    media: Media
  ): Playable<YouTubePlayerView> {
    return YouTubeAPlayable(master, media, config, YouTubeBridge(media))
  }

  override fun createRebinder(tag: Any): Rebinder<YouTubePlayerView> {
    return YouTubeARebinder(tag)
  }
}
