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

package kohii.v1.sample.ui.overlay

import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.core.Common
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.data.Video
import kohii.v1.sample.svg.GlideApp

internal class VideoItemsAdapter(
  private val videos: List<Video>,
  private val kohii: Kohii,
  val shouldBindVideo: (Rebinder?) -> Boolean,
  val onVideoClick: (Int, Rebinder) -> Unit
) : Adapter<BaseViewHolder>(), BaseViewHolder.OnClickListener {

  init {
    setHasStableIds(true)
  }

  override fun onItemClick(
    itemView: View,
    transView: View?,
    adapterPos: Int,
    itemId: Long,
    payload: Any?
  ) {
    onVideoClick(adapterPos, payload as Rebinder)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder = VideoItemHolder(
      from(parent.context),
      parent,
      this
  )

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE / 2
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    val item = videos[position % videos.size]
    if (holder is VideoItemHolder) {
      holder.videoData = item
      holder.videoTitle.text = item.title
      holder.videoInfo.text = item.description

      GlideApp.with(holder.itemView)
          .load(requireNotNull(holder.videoImage))
          .into(holder.thumbnail)

      if (shouldBindVideo(holder.rebinder)) {
        kohii.setUp(requireNotNull(holder.videoFile)) {
          tag = requireNotNull(holder.videoTag)
          repeatMode = Common.REPEAT_MODE_ONE
          artworkHintListener = holder
        }
            .bind(holder.playerViewContainer)
      }
    }
  }

  override fun onViewAttachedToWindow(holder: BaseViewHolder) {
    super.onViewAttachedToWindow(holder)
    holder.onAttached()
  }

  override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
    super.onViewDetachedFromWindow(holder)
    holder.onDetached()
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    if (holder is VideoItemHolder) {
      kohii.cancel(holder.playerViewContainer)
    }
    holder.onRecycled()
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.clearTransientStates()
    return true
  }
}
