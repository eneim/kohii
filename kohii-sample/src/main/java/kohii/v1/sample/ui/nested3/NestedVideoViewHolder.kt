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

package kohii.v1.sample.ui.nested3

import android.view.ViewGroup
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R.id
import kohii.v1.sample.R.layout
import kohii.v1.sample.common.BaseViewHolder

internal class NestedVideoViewHolder(
  private val kohii: Kohii,
  parent: ViewGroup
) : BaseViewHolder(
  parent,
  layout.holder_player_view
) {

  val container = itemView.findViewById(
    id.playerContainer
  ) as AspectRatioFrameLayout
  val playerView = itemView.findViewById(
    id.playerView
  ) as PlayerView

  override fun bind(item: Any?) {
    super.bind(item)
    container.setAspectRatio(16 / 9F)
    kohii.setUp(assetVideoUri) {
      tag = "NESTED::VID::$adapterPosition"
    }
      .bind(playerView)
  }
}
