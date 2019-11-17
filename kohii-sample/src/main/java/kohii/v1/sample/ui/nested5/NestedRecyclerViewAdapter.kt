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

package kohii.v1.sample.ui.nested5

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.core.Master
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.data.Item

/**
 * @author eneim (2018/07/06).
 */
class NestedRecyclerViewAdapter(
  private val parentPosition: Int,
  private val items: List<Item>,
  private val kohii: Master
) : Adapter<BaseViewHolder>() {

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_mix_view -> VideoViewHolder(parent, kohii)
      else -> throw RuntimeException("Unknown type: $viewType")
    }
  }

  override fun getItemCount() = items.size

  override fun getItemId(position: Int): Long {
    val item = items[position]
    return item.hashCode()
        .toLong()
  }

  override fun getItemViewType(position: Int): Int {
    return R.layout.holder_mix_view
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(parentPosition to items[position % items.size])
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
