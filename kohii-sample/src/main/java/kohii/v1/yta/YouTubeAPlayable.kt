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
import kohii.media.Media
import kohii.v1.Bridge

class YouTubeAPlayable(
  master: Master,
  media: Media,
  config: Config,
  bridge: Bridge<YouTubePlayerView>
) : Playable<YouTubePlayerView>(master, media, config, YouTubePlayerView::class.java, bridge) {

  override fun onConfigChange(): Boolean {
    super.onPause()
    return false
  }
}
