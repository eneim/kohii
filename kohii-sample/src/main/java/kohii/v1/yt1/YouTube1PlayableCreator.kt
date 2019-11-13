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

package kohii.v1.yt1

import kohii.core.Master
import kohii.core.Playable
import kohii.core.Playable.Config
import kohii.core.PlayableCreator
import kohii.core.Rebinder
import kohii.media.Media
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
class YouTube1Rebinder(
  override val tag: @RawValue Any
) : Rebinder<YouTubePlayerFragment>(tag, YouTubePlayerFragment::class.java)

class YouTube1PlayableCreator : PlayableCreator<YouTubePlayerFragment>(
    YouTubePlayerFragment::class.java
) {

  override fun createPlayable(
    master: Master,
    config: Config,
    media: Media
  ): Playable<YouTubePlayerFragment> {
    return YouTube1Playable(master, media, config, YouTubeBridge(media))
  }

  override fun createRebinder(tag: Any): Rebinder<YouTubePlayerFragment> {
    return YouTube1Rebinder(tag)
  }
}
