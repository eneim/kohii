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

package kohii.core

import android.view.LayoutInflater
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.R

internal class PlayerViewProvider : ViewRendererProvider() {

  override fun getMediaType(media: Media): Int {
    return if (media.mediaDrm != null) R.layout.kohii_player_surface_view else R.layout.kohii_player_textureview
  }

  override fun createRenderer(
    playback: Playback,
    mediaType: Int
  ): PlayerView {
    return LayoutInflater.from(playback.container.context)
        .inflate(mediaType, playback.container, false) as PlayerView
  }
}
