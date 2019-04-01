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
import com.google.android.exoplayer2.trackselection.TrackSelector
import kohii.media.Media
import kohii.v1.Cleanable

/**
 * @author eneim (2018/10/27).
 */
interface ExoPlayerProvider : Cleanable {

  fun acquirePlayer(media: Media): Player

  fun releasePlayer(
    media: Media,
    player: Player
  )

  val trackSelector: TrackSelector
}
