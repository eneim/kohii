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

package kohii.v1.sample.ui.pagers

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.v1.core.Playback
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

internal class VideoViewHolder(
  parent: ViewGroup,
  private val pagePos: Int
) : BaseViewHolder(parent, R.layout.holder_player_container), Playback.StateListener {

  internal val content = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
  internal val container = itemView.findViewById(R.id.container) as ViewGroup
  internal val thumbnail = itemView.findViewById(R.id.thumbnail) as View

  init {
    content.setAspectRatio(16 / 9F)
  }

  internal var videoUrl: String? = null

  internal val videoTag: String?
    get() = videoUrl?.let { "GRID::$pagePos::〜$adapterPosition" }

  override fun beforePlay(playback: Playback) {
    thumbnail.isVisible = false
  }

  override fun afterPause(playback: Playback) {
    thumbnail.isVisible = true
  }
}
