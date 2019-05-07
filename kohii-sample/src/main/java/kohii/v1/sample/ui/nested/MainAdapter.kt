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

package kohii.v1.sample.ui.nested

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.util.putAll
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.Kohii
import kohii.v1.sample.R

class MainAdapter(
  val kohii: Kohii,
  private val fragment: Fragment,
  private val items: List<Item>
) : Adapter<MainViewHolder>() {

  companion object {
    const val TYPE_NORMAL = R.layout.holder_nested_normal
    const val TYPE_LIST = R.layout.holder_nested_recyclereview
    const val STATE_KEY = "nested:adapter:state"
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): MainViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(viewType, parent, false)
    return if (viewType == TYPE_LIST) NestRvViewHolder(kohii, view) else MainViewHolder(view)
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 1 || position == 4) TYPE_LIST else TYPE_NORMAL
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  private val positionCache = SparseArray<HolderStateEntry>()

  fun onRestoreState(state: Bundle) {
    val temp =
      state.getSparseParcelableArray<HolderStateEntry>(STATE_KEY) as SparseArray<HolderStateEntry>
    this.positionCache.clear()
    this.positionCache.putAll(temp)
  }

  override fun onBindViewHolder(
    holder: MainViewHolder,
    position: Int
  ) {
    holder.bind(position)
    if (holder is NestRvViewHolder) {
      kohii.register(fragment, arrayOf(holder.container))
      val adapter = ItemsAdapter(items, kohii)
      holder.container.adapter = adapter

      if (holder.container.onFlingListener == null) {
        PagerSnapHelper().attachToRecyclerView(holder.container)
      }
    }
  }

  override fun onViewAttachedToWindow(holder: MainViewHolder) {
    super.onViewAttachedToWindow(holder)
    if (holder is NestRvViewHolder) {
      val pair = positionCache.get(holder.adapterPosition, null)
      positionCache.remove(holder.adapterPosition)
      if (pair != null) {
        (holder.container.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(pair.first!!, pair.second!!)
      }
    }
  }

  override fun onViewDetachedFromWindow(holder: MainViewHolder) {
    super.onViewDetachedFromWindow(holder)
    if (holder is NestRvViewHolder) {
      val layout = holder.container.layoutManager as LinearLayoutManager
      val childPos = layout.findFirstVisibleItemPosition()
      val childHolder = holder.container.findViewHolderForAdapterPosition(childPos)
      if (childHolder?.itemView != null) {
        var childLeft = layout.getDecoratedLeft(childHolder.itemView)
        (childHolder.itemView.layoutParams as? MarginLayoutParams)?.also {
          childLeft -= it.marginStart
        }
        childLeft -= holder.container.paddingStart
        positionCache.put(holder.adapterPosition, HolderStateEntry(childPos, childLeft))
      }
    }
  }
}
