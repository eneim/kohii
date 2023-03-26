/*
 * Copyright (c) 2023 Nam Nguyen, nam@ene.im
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

package kohii.v1.exoplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v1.core.Playback
import kohii.v1.core.ViewRendererProvider
import kohii.v1.media.Media

class StyledPlayerViewProvider : ViewRendererProvider() {

  override fun getRendererType(
    container: ViewGroup,
    media: Media
  ): Int {
    // Note: we want to use SurfaceView on API 24 and above. But reusing SurfaceView doesn't seem to
    // be straight forward, as it is not trivial to clean the cache of old video ...
    return if (media.mediaDrm != null /* || Build.VERSION.SDK_INT >= 24 */) {
      R.layout.kohii_styled_player_surface_view
    } else {
      R.layout.kohii_styled_player_texture_view
    }
  }

  override fun createRenderer(
    playback: Playback,
    rendererType: Int
  ): StyledPlayerView {
    return LayoutInflater.from(playback.container.context)
      .inflate(rendererType, playback.container, false) as StyledPlayerView
  }
}
