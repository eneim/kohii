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

package kohii.v1.yt2

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.core.AbstractPlayable
import kohii.core.Master
import kohii.media.Media
import kohii.v1.Bridge

class YouTube2Playable(
  master: Master,
  media: Media,
  config: Config,
  bridge: Bridge<YouTubePlayerView>
) : AbstractPlayable<YouTubePlayerView>(master, media, config, bridge) {

  override fun shouldAttachRenderer(renderer: Any?) {
    if (renderer is YouTubePlayerView) bridge.renderer = renderer
  }

  override fun shouldDetachRenderer() {
    bridge.renderer = null
  }

  override fun onConfigChange() = false
}
