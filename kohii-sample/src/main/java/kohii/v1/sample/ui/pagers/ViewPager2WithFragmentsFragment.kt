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
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.getDisplayPoint
import kohii.v1.sample.data.Video
import kohii.v1.sample.databinding.FragmentPager2HorizontalBinding
import kohii.v1.sample.ui.main.DemoItem
import kotlin.math.abs

// ViewPager2 whose pages are Fragments
class ViewPager2WithFragmentsFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() =
      ViewPager2WithFragmentsFragment()
  }

  class VideoPagerAdapter(
    private val videos: List<Video>,
    fragment: Fragment
  ) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = Int.MAX_VALUE / 2

    override fun createFragment(position: Int): Fragment {
      return PageFragment.newInstance(position, videos[position % videos.size])
    }
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_pager_2_horizontal, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentPager2HorizontalBinding = FragmentPager2HorizontalBinding.bind(view)
    Kohii[this].register(this)
      .addBucket(binding.viewPager)

    binding.viewPager.apply {
      adapter = VideoPagerAdapter(getApp().videos, this@ViewPager2WithFragmentsFragment)
      val pageMargin = resources.getDimensionPixelSize(R.dimen.pager_horizontal_space_base)
      val pageTransformer = CompositePageTransformer()
      pageTransformer.addTransformer(MarginPageTransformer(pageMargin))

      val clientWidth = (requireActivity().getDisplayPoint().x - paddingStart - paddingEnd)
      val offset = paddingStart / clientWidth.toFloat()
      pageTransformer.addTransformer { page, position ->
        val scale = (1f - abs(position - offset) * 0.15f).coerceAtLeast(0.5f)
        page.scaleX = scale
        page.scaleY = scale
      }

      setPageTransformer(pageTransformer)
    }
  }
}
