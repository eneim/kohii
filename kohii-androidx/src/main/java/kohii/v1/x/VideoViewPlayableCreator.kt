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

package kohii.v1.x

import androidx.media2.player.MediaPlayer
import androidx.media2.widget.VideoView
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playable.Config
import kohii.v1.core.PlayableCreator
import kohii.v1.core.PlayerPool
import kohii.v1.media.Media

/**
 * Default implementation of [PlayableCreator] that supports [VideoView]
 */
class VideoViewPlayableCreator @JvmOverloads constructor(
  private val master: Master,
  private val playerPool: PlayerPool<MediaPlayer> = MediaPlayerPool(master.app)
) : PlayableCreator<VideoView>(VideoView::class.java) {

  override fun createPlayable(
    config: Config,
    media: Media
  ): Playable {
    return VideoViewPlayable(
        master,
        media,
        config,
        VideoViewBridge(media, playerPool)
    )
  }

  override fun cleanUp() {
    playerPool.clear()
  }
}
