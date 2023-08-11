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

package kohii.v1.sample.ui.nested3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import kohii.v1.core.MemoryMode
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.databinding.FragmentDebugNestsvInRvBinding
import kohii.v1.sample.ui.main.DemoItem

class NestedScrollViewInsideRecyclerViewFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = NestedScrollViewInsideRecyclerViewFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_nestsv_in_rv, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentDebugNestsvInRvBinding = FragmentDebugNestsvInRvBinding.bind(view)
    val kohii = Kohii[this]
    val manager = kohii.register(this, MemoryMode.BALANCED)
      .addBucket(binding.recyclerView)

    binding.recyclerView.adapter = NestedItemsAdapter(kohii, manager)

    // To allow NestedScrollView to scroll inside RecyclerView.
    // This implementation is really simple and should not be used as-is in production code.
    binding.recyclerView.addOnItemTouchListener(object : SimpleOnItemTouchListener() {

      override fun onInterceptTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ): Boolean {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return false
        val holder = rv.findContainingViewHolder(child) ?: return false
        if (holder !is NestedScrollViewHolder) return false
        child.parent.requestDisallowInterceptTouchEvent(true)
        return false
      }
    })
  }
}
