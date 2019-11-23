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
import kohii.v1.core.AbstractPlayable
import kohii.v1.core.Bridge
import kohii.v1.core.Master
import kohii.v1.media.Media

class YouTube2Playable(
  master: Master,
  media: Media,
  config: Config,
  bridge: Bridge<YouTubePlayerView>
) : AbstractPlayable<YouTubePlayerView>(master, media, config, bridge) {

  override var renderer: Any?
    get() = bridge.renderer
    set(value) {
      require(value is YouTubePlayerView?)
      bridge.renderer = value
    }

  override fun onConfigChange() = false
}
