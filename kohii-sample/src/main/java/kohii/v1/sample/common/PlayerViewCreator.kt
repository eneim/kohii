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

package kohii.v1.sample.common

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.OutputHolderCreator
import kohii.v1.R

class PlayerViewCreator : OutputHolderCreator<ViewGroup, PlayerView> {

  companion object {
    val instance = PlayerViewCreator()
  }

  override fun getMediaType(media: Media): Int {
    return if (media.mediaDrm != null) R.layout.kohii_player_surface_view else R.layout.kohii_player_textureview
  }

  override fun createOutputHolder(
    container: ViewGroup,
    type: Int
  ): PlayerView {
    require(
        type == R.layout.kohii_player_textureview ||
            type == R.layout.kohii_player_surface_view ||
            type == R.layout.kohii_player_spherical_view
    ) { "Unknown type: $type" }

    return LayoutInflater.from(container.context).inflate(type, container, false) as PlayerView
  }
}
