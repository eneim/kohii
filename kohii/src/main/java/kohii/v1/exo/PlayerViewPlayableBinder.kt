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

package kohii.v1.exo

import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableBinder

/**
 * @author eneim (2019/03/14)
 *
 * Need to give the Config more power. Because Playable lives at global scope, so is instance of this class.
 * Responsibilities:
 * - Allow binding to specific type of Target.
 * - Judge from the target type to build correct Playable.
 */
internal class PlayerViewPlayableBinder(
  kohii: Kohii
) : PlayableBinder<PlayerView>(kohii, PlayerView::class.java) {

  override fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Config
  ): Playable<PlayerView> {
    return PlayerViewPlayable(kohii, media, config)
  }
}
