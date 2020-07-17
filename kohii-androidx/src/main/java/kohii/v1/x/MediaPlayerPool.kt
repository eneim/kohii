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

import android.content.Context
import androidx.media2.player.MediaPlayer
import kohii.v1.core.PlayerPool
import kohii.v1.media.Media

/**
 * A [PlayerPool] for the [MediaPlayer].
 */
open class MediaPlayerPool(private val context: Context) : PlayerPool<MediaPlayer>() {

  override fun recyclePlayerForMedia(media: Media): Boolean = media.mediaDrm == null

  override fun createPlayer(media: Media): MediaPlayer = MediaPlayer(context)

  override fun resetPlayer(player: MediaPlayer) = player.reset()

  override fun destroyPlayer(player: MediaPlayer) = player.close()
}
