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

package kohii.v1.sample.ui.fbook.vh

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.data.Sources
import kohii.v1.sample.data.Video

internal class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Kohii
) : FbookItemHolder(parent) {

  init {
    videoContainer.isVisible = true
  }

  private val playerView = itemView.findViewById(R.id.playerView) as PlayerView
  private val thumbnail = itemView.findViewById(R.id.thumbnail) as ImageView

  var videoSources: Sources? = null

  val tagKey: String?
    get() = this.videoSources?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  val rebinder: Rebinder?
    get() = this.tagKey?.let { Rebinder(it, PlayerView::class.java) }

  override fun bind(item: Any?) {
    super.bind(item)
    (item as? Video)?.also {
      this.videoSources = it.playlist.first()
          .also { pl ->
            Glide.with(itemView)
                .load(pl.image)
                .into(thumbnail)
          }
          .sources.first()

      kohii.setUp(videoSources!!.file)
          .with {
            tag = tagKey
            repeatMode = Playable.REPEAT_MODE_ONE
          }
          .bind(playerView)
    }
  }
}
