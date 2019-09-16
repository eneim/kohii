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

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Rebinder
import kohii.v1.sample.R

class VideoViewHolder(itemView: View) : ViewHolder(itemView) {

  internal val playerContainer = itemView.findViewById<AspectRatioFrameLayout>(R.id.playerContainer)
  internal val videoTitle = itemView.findViewById<TextView>(R.id.videoTitle)

  init {
    playerContainer.setAspectRatio(2.0F)
  }

  var rebinder: Rebinder<PlayerView>? = null
  var aspectRatio: Float = 1F
}
