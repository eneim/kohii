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

package kohii.v1.sample.ui.pagers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.getDisplayPoint
import kohii.v1.sample.databinding.FragmentPagerBinding
import kohii.v1.sample.ui.main.DemoItem
import kotlin.math.abs

// ViewPager (1) whose pages are Fragments
class ViewPager1WithRecyclerViewFragmentsFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = ViewPager1WithRecyclerViewFragmentsFragment()
  }

  internal class VideoPagerAdapter(fm: FragmentManager) :
      FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
      return GridContentFragment.newInstance(position)
    }

    override fun getCount() = Int.MAX_VALUE / 2
  }

  private lateinit var binding: FragmentPagerBinding
  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding: FragmentPagerBinding = FragmentPagerBinding.inflate(inflater, container, false)
    this.binding = binding
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    Kohii[this].register(this)
        .addBucket(binding.viewPager)
    binding.viewPager.apply {
      adapter = VideoPagerAdapter(childFragmentManager)
      setPadding(0)
    }
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    binding.viewPager.apply {
      val clientWidth = (requireActivity().getDisplayPoint().x - paddingStart - paddingEnd)
      val offset = paddingStart / clientWidth.toFloat()
      setPageTransformer(false) { page, position ->
        val scale = (1f - abs(position - offset) * 0.15f).coerceAtLeast(0.5f)
        page.scaleX = scale
        page.scaleY = scale
      }
    }
  }
}
