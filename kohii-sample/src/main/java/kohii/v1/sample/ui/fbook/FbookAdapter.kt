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

package kohii.v1.sample.ui.fbook

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.PlaybackManager
import kohii.v1.sample.data.Video
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder.OnClick
import kohii.v1.sample.ui.fbook.vh.PhotoViewHolder
import kohii.v1.sample.ui.fbook.vh.TextViewHolder
import kohii.v1.sample.ui.fbook.vh.VideoViewHolder

internal class FbookAdapter(
  val kohii: Kohii,
  val manager: PlaybackManager,
  val videos: List<Video>,
  val fragment: FbookFragment,
  val onClick: OnClick
) : Adapter<FbookItemHolder>() {

  companion object {
    const val TYPE_TEXT = 100
    const val TYPE_PHOTO = 200
    const val TYPE_VIDEO = 300
  }

  init {
    setHasStableIds(true)
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemViewType(position: Int): Int {
    val key = position % 5
    return if (key == 0 || key == 4) TYPE_VIDEO
    else if (key == 1 || key == 2) TYPE_TEXT
    else TYPE_PHOTO
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): FbookItemHolder {
    val result = when (viewType) {
      TYPE_TEXT -> TextViewHolder(parent)
      TYPE_PHOTO -> PhotoViewHolder(parent)
      TYPE_VIDEO -> VideoViewHolder(parent, kohii, manager) {
        fragment.currentPlayerInfo?.rebinder != it
      }
      else -> throw IllegalArgumentException("Unknown type: $viewType")
    }

    result.setupOnClick(onClick)
    return result
  }

  override fun getItemCount() = Int.MAX_VALUE

  override fun onBindViewHolder(
    holder: FbookItemHolder,
    position: Int
  ) {
    if (getItemViewType(position) == TYPE_VIDEO) {
      val video = videos[((position + 1) / 5) % videos.size]
      holder.bind(video)
    } else {
      holder.bind(position)
    }
  }

  override fun onBindViewHolder(
    holder: FbookItemHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    val payload = payloads.firstOrNull { it is VolumeInfo }
    if (payload != null && holder is VideoViewHolder) {
      holder.volume.isSelected = !(payload as VolumeInfo).mute
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun onViewAttachedToWindow(holder: FbookItemHolder) {
    super.onViewAttachedToWindow(holder)
    (holder as? VideoViewHolder)?.dispatchBindVideo()
  }
}
