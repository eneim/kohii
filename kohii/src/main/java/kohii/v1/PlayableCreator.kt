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

package kohii.v1

import android.net.Uri
import kohii.media.Media
import kohii.media.MediaItem

// Use this instead of Kohii instance to provide more customizable detail.
abstract class PlayableCreator<RENDERER : Any>(
  protected val kohii: Kohii,
  internal val rendererType: Class<RENDERER>
) {

  fun setUp(uri: Uri) = this.setUp(MediaItem(uri))

  fun setUp(url: String) = this.setUp(Uri.parse(url))

  fun setUp(media: Media): Binder<RENDERER> {
    return Binder(kohii, media, this)
  }

  abstract fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ): Playable<RENDERER>
}
