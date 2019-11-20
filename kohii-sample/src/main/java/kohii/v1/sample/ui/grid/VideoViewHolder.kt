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
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

internal class VideoViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_player_container), Playback.StateListener {

  private val root: AspectRatioFrameLayout = itemView.findViewById(R.id.playerContainer)
  internal val container: FrameLayout = itemView.findViewById(R.id.container)
  internal val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)

  init {
    root.setAspectRatio(16 / 9F)
  }

  internal var videoUrl: String? = null

  internal val videoTag: String?
    get() = videoUrl?.let { "HOLDER::〜$adapterPosition" }

  internal val rebinder: Rebinder?
    get() = videoTag?.let { Rebinder(it) }

  internal val itemDetails: ItemDetails<Rebinder>
    get() = object : ItemDetails<Rebinder>() {
      override fun getSelectionKey() = rebinder
      override fun getPosition() = adapterPosition
    }

  override fun beforePlay(playback: Playback) {
    thumbnail.isVisible = false
  }

  override fun afterPause(playback: Playback) {
    thumbnail.isVisible = true
  }

  override fun onEnded(playback: Playback) {
    thumbnail.isVisible = true
  }
}
