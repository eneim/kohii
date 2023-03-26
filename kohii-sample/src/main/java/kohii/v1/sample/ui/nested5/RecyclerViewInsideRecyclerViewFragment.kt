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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.util.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding
import kohii.v1.sample.ui.main.DemoItem
import kohii.v1.sample.ui.nested5.MainAdapter.Companion.STATE_KEY

class RecyclerViewInsideRecyclerViewFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = RecyclerViewInsideRecyclerViewFragment()
  }

  private lateinit var binding: FragmentRecyclerViewBinding
  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding: FragmentRecyclerViewBinding =
      FragmentRecyclerViewBinding.inflate(inflater, container, false)
    this.binding = binding
    return binding.root
  }

  private lateinit var adapter: MainAdapter

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val kohii = Kohii[this]
    val manager = kohii.register(this)
      .addBucket(binding.recyclerView)

    adapter = MainAdapter(kohii, manager, getApp().exoItems)
    if (savedInstanceState != null) adapter.onRestoreState(savedInstanceState)

    binding.recyclerView.adapter = adapter
    val layoutManager = LinearLayoutManager(requireContext())
    layoutManager.isItemPrefetchEnabled = true
    binding.recyclerView.layoutManager = layoutManager
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val positionCache = SparseArray<HolderStateEntry>()
    val nestedRecyclerViews =
      binding.recyclerView.findHoldersForType<NestedRecyclerViewViewHolder>()
    if (nestedRecyclerViews.isNotEmpty()) {
      nestedRecyclerViews.forEach { holder ->
        val layout = holder.container.layoutManager as LinearLayoutManager
        val childPos = layout.findFirstVisibleItemPosition()
        val childHolder = holder.container.findViewHolderForAdapterPosition(childPos)
        if (childHolder?.itemView != null) {
          var childLeft = layout.getDecoratedLeft(childHolder.itemView)
          (childHolder.itemView.layoutParams as? MarginLayoutParams)?.also {
            childLeft -= it.marginStart
          }
          childLeft -= holder.container.paddingStart
          positionCache.put(holder.absoluteAdapterPosition, HolderStateEntry(childPos, childLeft))
        }
      }
    }

    // Now put the state of those are detached.
    adapter.holderStateCache.forEach { key, value ->
      positionCache.put(key, value)
    }
    outState.putSparseParcelableArray(STATE_KEY, positionCache)
  }
}

internal inline fun <reified R : ViewHolder> RecyclerView.findHoldersForType(): List<R> {
  val result = ArrayList<R>()
  val layout = layoutManager ?: return result
  if (layout.childCount > 0) {
    for (index in 0 until layout.childCount) {
      val view = layout.getChildAt(index)
      val holder = if (view != null) this.findContainingViewHolder(view) else null
      if (holder != null && holder is R) {
        result.add(holder)
      }
    }
  }

  return result
}
