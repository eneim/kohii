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

package kohii.v1.sample.ui.youtube

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.google.api.services.youtube.model.Video
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.core.Engine
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

class YouTubePlaylistPagingAdapter(
  private val engine: Engine<YouTubePlayerView>
) : PagingDataAdapter<Video, BaseViewHolder>(object : DiffUtil.ItemCallback<Video>() {
  override fun areItemsTheSame(
    oldItem: Video,
    newItem: Video
  ): Boolean {
    return newItem.id === oldItem.id
  }

  override fun areContentsTheSame(
    oldItem: Video,
    newItem: Video
  ): Boolean {
    return newItem.statistics == oldItem.statistics
  }
}) {

  override fun getItemViewType(position: Int): Int {
    val item = peek(position)
    return if (item == null) {
      R.layout.holder_loading
    } else {
      R.layout.holder_youtube_container
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_youtube_container -> YouTubeViewHolder(parent, viewType)
      R.layout.holder_loading -> BaseViewHolder(parent, viewType)
      else -> throw IllegalArgumentException("unknown view type $viewType")
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    if (holder is YouTubeViewHolder) {
      val item = getItem(position)
      holder.bind(item)
      val videoId = item?.id ?: "EOjq4OIWKqM"
      engine.setUp(videoId) {
        tag = videoId
        threshold = 0.999F
        artworkHintListener = holder
      }
        .bind(holder.container) {
          holder.playback = it
        }
    }
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    holder.onRecycled(true)
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.onRecycled(false)
    return super.onFailedToRecycleView(holder)
  }
}
