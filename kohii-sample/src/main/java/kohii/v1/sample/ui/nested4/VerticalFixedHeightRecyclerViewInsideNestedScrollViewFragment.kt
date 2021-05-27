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

package kohii.v1.sample.ui.nested4

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.GridLayoutManager
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.databinding.FragmentDebugRvInNestsvVerticalBinding
import kohii.v1.sample.ui.main.DemoItem

class VerticalFixedHeightRecyclerViewInsideNestedScrollViewFragment : BaseFragment(),
    DemoContainer {

  companion object {
    fun newInstance() = VerticalFixedHeightRecyclerViewInsideNestedScrollViewFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_rv_in_nestsv_vertical, container, false)
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentDebugRvInNestsvVerticalBinding =
      FragmentDebugRvInNestsvVerticalBinding.bind(view)
    val kohii = Kohii[this]
    kohii.register(this)
        .addBucket(binding.scrollView)
        .addBucket(binding.recyclerView)

    binding.libIntro.text = getString(R.string.lib_intro).parseAsHtml()

    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int {
        return if (position % 6 == 3) 2 else 1
      }
    }

    // Constraint RecyclerView height
    binding.recyclerView.layoutParams.height = LayoutParams.WRAP_CONTENT
    binding.recyclerViewContainer.layoutParams.height = LayoutParams.WRAP_CONTENT

    binding.recyclerViewTitle.text = "Below is a fixed height RecyclerView"

    // Setup RecyclerView
    binding.recyclerView.isNestedScrollingEnabled = false // test
    (binding.recyclerView.layoutManager as? GridLayoutManager)?.let {
      it.spanCount = 3
      it.spanSizeLookup = spanSizeLookup
    }
    binding.recyclerView.adapter = ItemsAdapter(kohii, 13)

    kohii.setUp(assetVideoUri)
        .bind(binding.dummyPlayer1)

    kohii.setUp(assetVideoUri)
        .bind(binding.dummyPlayer2)
  }
}
