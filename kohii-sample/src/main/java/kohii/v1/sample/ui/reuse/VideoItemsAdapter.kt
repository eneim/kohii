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

package kohii.v1.sample.ui.reuse

import android.view.LayoutInflater
import android.view.LayoutInflater.from
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.sample.R
import kohii.v1.sample.ui.reuse.data.Video

internal class VideoItemsAdapter(
  private val videos: List<Video>,
  private val lifecycleOwner: LifecycleOwner
) : Adapter<BaseViewHolder>(), PlayerManager {

  var playerView: PlayerView? = null
  var activeHolder: VideoItemHolder? = null

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    playerView = LayoutInflater.from(recyclerView.context)
        .inflate(R.layout.playerview_texture, recyclerView, false) as PlayerView
    playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    playerView = null
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return VideoItemHolder(
        from(parent.context),
        R.layout.holder_video_text_reuse,
        parent,
        lifecycleOwner
    )
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(videos[position % videos.size])
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    holder.onRecycled(true)
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.onRecycled(false)
    return true
  }

  override fun play(viewHolder: VideoItemHolder) {
    if (activeHolder === viewHolder) return
    activeHolder?.unbindView(this.playerView)
    playerView?.let {
      viewHolder.bindView(it)
      activeHolder = viewHolder
    }
  }

  override fun pause(viewHolder: VideoItemHolder) {
    viewHolder.unbindView(this.playerView)
    if (activeHolder?.adapterPosition == viewHolder.adapterPosition) activeHolder = null
  }

  override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
    super.onViewDetachedFromWindow(holder)
    (holder as? VideoItemHolder)?.let {
      pause(it)
    }
  }
}