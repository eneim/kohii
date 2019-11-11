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

package kohii.v1.sample.ui.combo

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

class ExoVideoHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_player_container) {

  internal val playerContainer =
    itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
  internal val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  internal val thumbnail = itemView.findViewById(R.id.thumbnail) as ImageView

  init {
    playerContainer.setAspectRatio(2.0F)
  }

  var rebinder: Rebinder<PlayerView>? = null
  var aspectRatio: Float = 1F
}
