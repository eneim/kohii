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

package kohii.v1.core

import android.view.View
import androidx.core.view.ViewCompat
import kohii.v1.media.Media

abstract class ViewRendererProvider(poolSize: Int) :
    RecycledRendererProvider(poolSize) {

  constructor() : this(2)

  override fun releaseRenderer(
    playback: Playback,
    media: Media,
    renderer: Any?
  ) {
    if (renderer != null && renderer !== playback.container) {
      require(
          renderer is View && renderer.parent == null && !ViewCompat.isAttachedToWindow(renderer)
      )
    }
    super.releaseRenderer(playback, media, renderer)
  }
}
