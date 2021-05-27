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

package kohii.v1.sample.ui.nested2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.PagerSnapHelper
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.databinding.FragmentDebugRvInNestsvHorizontalBinding
import kohii.v1.sample.ui.main.DemoItem

class HorizontalRecyclerViewInsideNestedScrollViewFragment : BaseFragment(), DemoContainer {

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  lateinit var snapHelper: PagerSnapHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_rv_in_nestsv_horizontal, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentDebugRvInNestsvHorizontalBinding =
      FragmentDebugRvInNestsvHorizontalBinding.bind(view)
    val kohii = Kohii[this]
    kohii.register(this)
        .addBucket(binding.scrollView)
        .addBucket(binding.recyclerView)

    binding.libIntro.text = getString(R.string.lib_intro).parseAsHtml()

    binding.recyclerView.adapter = HorizontalItemsAdapter(kohii)
    snapHelper = PagerSnapHelper()
    snapHelper.attachToRecyclerView(binding.recyclerView)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    snapHelper.attachToRecyclerView(null)
  }
}
