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

import android.os.Bundle
import android.util.SparseArray
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.util.putAll
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.core.Manager
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.common.inflateView
import kohii.v1.sample.data.Item

class MainAdapter(
  val kohii: Kohii,
  val manager: Manager,
  private val items: List<Item>
) : Adapter<BaseViewHolder>() {

  companion object {
    const val TYPE_NORMAL = R.layout.holder_nested_normal
    const val TYPE_LIST = R.layout.holder_nested_recyclerview
    const val STATE_KEY = "${BuildConfig.APPLICATION_ID}::nested::state"
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    val view = parent.inflateView(viewType)
    return if (viewType == TYPE_LIST) NestedRecyclerViewViewHolder(view) else MainViewHolder(view)
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 1 || position == 6) TYPE_LIST else TYPE_NORMAL
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  internal val holderStateCache = SparseArray<HolderStateEntry>()

  fun onRestoreState(state: Bundle) {
    val temp: SparseArray<HolderStateEntry>? = state.getSparseParcelableArray(STATE_KEY)
    this.holderStateCache.clear()
    if (temp != null) this.holderStateCache.putAll(temp)
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(position)
    if (holder is NestedRecyclerViewViewHolder) {
      manager.attach(holder.container)
      val adapter = NestedRecyclerViewAdapter(position, items, kohii)
      holder.container.adapter = adapter

      if (holder.container.onFlingListener == null) {
        PagerSnapHelper().attachToRecyclerView(holder.container)
      }
    }
  }

  override fun onViewAttachedToWindow(holder: BaseViewHolder) {
    super.onViewAttachedToWindow(holder)
    if (holder is NestedRecyclerViewViewHolder) {
      val stateEntry = holderStateCache.get(holder.adapterPosition, HolderStateEntry(0, 0))
      holderStateCache.remove(holder.adapterPosition)
      val (position, offset) = stateEntry
      holder.container.post {
        holder.layoutManager.scrollToPositionWithOffset(position, offset)
      }
    }
  }

  override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
    super.onViewDetachedFromWindow(holder)
    if (holder is NestedRecyclerViewViewHolder) {
      val layout = holder.layoutManager
      val firstChildPos = layout.findFirstVisibleItemPosition()
      val childHolder = holder.container.findViewHolderForAdapterPosition(firstChildPos)
      if (childHolder?.itemView != null) {
        var childLeft =
          layout.getDecoratedLeft(childHolder.itemView) - holder.container.paddingStart
        val childParams = childHolder.itemView.layoutParams
        if (childParams is MarginLayoutParams) childLeft -= childParams.marginStart
        holderStateCache.put(holder.adapterPosition, HolderStateEntry(firstChildPos, childLeft))
      }
    }
  }
}
