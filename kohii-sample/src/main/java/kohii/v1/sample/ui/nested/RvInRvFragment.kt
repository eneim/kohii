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
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kohii.v1.Kohii
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView

class RvInRvFragment : BaseNestedFragment() {

  companion object {
    fun newInstance() = RvInRvFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, container, false)
  }

  lateinit var adapter: MainAdapter
  lateinit var layoutManager: LinearLayoutManager

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val kohii = Kohii[this]
    kohii.register(this, recyclerView)

    adapter = MainAdapter(kohii, this, videos)
    if (savedInstanceState != null) adapter.onRestoreState(savedInstanceState)

    recyclerView.adapter = adapter
    layoutManager = LinearLayoutManager(requireContext())
    layoutManager.isItemPrefetchEnabled = true
    recyclerView.layoutManager = layoutManager
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    @Suppress("UNCHECKED_CAST")
    val nestedHolders =
      recyclerView.filterViewHolder { it is NestRvViewHolder } as List<NestRvViewHolder>

    if (nestedHolders.isNotEmpty()) {
      val positionCache = SparseArray<HolderStateEntry>()
      nestedHolders.forEach { holder ->
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
      outState.putSparseParcelableArray(MainAdapter.STATE_KEY, positionCache)
    }
  }
}

fun RecyclerView.filterViewHolder(predicate: (ViewHolder) -> Boolean): List<ViewHolder> {
  val result = ArrayList<ViewHolder>()
  val layout = this.layoutManager ?: return result
  if (layout.childCount > 0) {
    for (index in 0 until layout.childCount) {
      val view = layout.getChildAt(index)
      val holder = if (view != null) this.findContainingViewHolder(view) else null
      if (holder != null && predicate.invoke(holder)) {
        result.add(holder)
      }
    }
  }

  return result
}
