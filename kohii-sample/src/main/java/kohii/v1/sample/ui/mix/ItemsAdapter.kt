/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.mix

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.LifecycleOwnerProvider
import kohii.v1.sample.R

/**
 * @author eneim (2018/07/06).
 */
class ItemsAdapter( //
  private val items: List<Item>, //
  private val kohii: Kohii,
  val containerProvider: ContainerProvider
) : Adapter<BaseViewHolder>() {

  private var inflater: LayoutInflater? = null

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    if (inflater == null || inflater!!.context != parent.context) {
      inflater = LayoutInflater.from(parent.context)
    }

    return when (viewType) {
      R.layout.holder_mix_view -> VideoViewHolder(inflater!!, parent, kohii, containerProvider)
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
    holder.bind(items[position % items.size])
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