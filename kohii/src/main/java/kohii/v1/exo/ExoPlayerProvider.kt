/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.exo

import com.google.android.exoplayer2.Player
import kohii.media.Media

/**
 * @author eneim (2018/10/27).
 *
 * A Pool to store unused Player instance. As initializing a Player is relatively expensive,
 * we try to cache them for reuse.
 */
interface ExoPlayerProvider {

  fun acquirePlayer(media: Media): Player

  fun releasePlayer(
    media: Media,
    player: Player
  )

  fun cleanUp()
}
