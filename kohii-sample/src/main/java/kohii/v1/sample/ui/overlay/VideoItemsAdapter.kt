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
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.Kohii
import kohii.v1.Rebinder
import kohii.v1.sample.data.Video

internal class VideoItemsAdapter(
  private val videos: List<Video>,
  private val kohii: Kohii
) : Adapter<BaseViewHolder>(), BaseViewHolder.OnClickListener {

  var selectionTracker: SelectionTracker<Rebinder<*>>? = null

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
    (payload as Rebinder<*>).also {
      if (selectionTracker?.isSelected(it) == true) return
      selectionTracker?.select(it)
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return VideoItemHolder(
        from(parent.context),
        parent,
        this,
        kohii,
        this
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
    holder.onRecycled(true)
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.onRecycled(false)
    return true
  }
}
