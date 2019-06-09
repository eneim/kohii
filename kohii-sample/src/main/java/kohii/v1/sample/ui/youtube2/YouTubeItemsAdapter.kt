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

package kohii.v1.sample.ui.youtube2

import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.google.api.services.youtube.model.Video
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.PlayableCreator
import kohii.v1.ViewTarget
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.youtube.data.NetworkState
import kohii.v1.ytb.YouTubePlayerFragment

class YouTubeItemsAdapter(
  private val creator: PlayableCreator<YouTubePlayerView>,
  private val fragmentManager: FragmentManager
) : PagedListAdapter<Video, BaseViewHolder>(object : DiffUtil.ItemCallback<Video>() {
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

  private var networkState: NetworkState? = null

  private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

  override fun getItemViewType(position: Int): Int {
    return if (hasExtraRow() && position == itemCount - 1) {
      R.layout.holder_loading // to loading type
    } else {
      R.layout.holder_youtube_container
    }
  }

  override fun getItemCount(): Int {
    return super.getItemCount() + if (hasExtraRow()) 1 else 0
  }

  fun setNetworkState(newNetworkState: NetworkState?) {
    val previousState = this.networkState
    val hadExtraRow = hasExtraRow()
    this.networkState = newNetworkState
    val hasExtraRow = hasExtraRow()
    if (hadExtraRow != hasExtraRow) {
      if (hadExtraRow) {
        notifyItemRemoved(super.getItemCount())
      } else {
        notifyItemInserted(super.getItemCount())
      }
    } else if (hasExtraRow && previousState != newNetworkState) {
      notifyItemChanged(itemCount - 1)
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_youtube_container -> YouTubeViewHolder(parent, viewType, fragmentManager)
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
      creator.setUp(videoId)
          .with {
            tag = videoId
            threshold = 0.99F
          }
          .bind(ViewTarget(holder.container)) {
            it.addPlaybackEventListener(holder)
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
