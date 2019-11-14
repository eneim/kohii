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

package kohii.v1.sample.ui.echo

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.core.Master
import kohii.media.VolumeInfo
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.data.Video

interface VolumeStore {

  fun get(key: Int): VolumeInfo

  fun set(
    key: Int,
    volumeInfo: VolumeInfo
  )
}

class VideoItemsAdapter(
  private val videos: List<Video>,
  private val kohii: Master,
  private val volumeStore: VolumeStore,
  internal val volumeInfoUpdater: (VideoItemHolder) -> VolumeInfo?
) : Adapter<BaseViewHolder>() {

  companion object {
    internal val PAYLOAD_VOLUME = Any()
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    val holder = VideoItemHolder(parent, kohii)

    holder.volumeButton.setOnClickListener {
      val updated = volumeInfoUpdater(holder)
      if (updated != null) notifyItemChanged(holder.adapterPosition, PAYLOAD_VOLUME)
    }

    return holder
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(position) // this action is ignored in this demo
    if (holder is VideoItemHolder) {
      holder.applyVideoData(videos[position % videos.size])
      holder.applyVolumeInfo(volumeStore.get(position))
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    val payload = payloads.find { it === PAYLOAD_VOLUME }
    if (payload != null && holder is VideoItemHolder) {
      holder.applyVolumeInfo(volumeStore.get(position))
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    holder.onRecycled(true)
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.onRecycled(false)
    return true
  }
}
