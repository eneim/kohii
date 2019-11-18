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

package kohii.v1.sample.ui.grid

import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.PlayerViewRebinder
import kohii.core.Rebinder
import kohii.v1.sample.R.id
import kohii.v1.sample.R.layout
import kohii.v1.sample.common.BaseViewHolder

internal class VideoViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, layout.holder_player_view) {
  internal val container = itemView.findViewById(
      id.playerContainer
  ) as AspectRatioFrameLayout

  init {
    container.setAspectRatio(16 / 9F)
  }

  internal var videoUrl: String? = null

  internal val videoTag: String?
    get() = videoUrl?.let { "HOLDER::〜$adapterPosition" }

  internal val rebinder: Rebinder<PlayerView>?
    get() = videoTag?.let { PlayerViewRebinder(it) }

  internal val itemDetails: ItemDetails<Rebinder<*>>
    get() = object : ItemDetails<Rebinder<*>>() {
      override fun getSelectionKey() = rebinder
      override fun getPosition() = adapterPosition
    }
}
