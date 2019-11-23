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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.getDisplayPoint
import kohii.v1.sample.data.Video
import kotlinx.android.synthetic.main.fragment_pager.viewPager
import kotlin.math.abs

// ViewPager (1) whose pages are Fragments
class ViewPager1WithFragmentsFragment : BaseFragment() {

  companion object {
    fun newInstance() =
      ViewPager1WithFragmentsFragment()
  }

  internal class VideoPagerAdapter(
    fm: FragmentManager,
    private val videos: List<Video>
  ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
      return PageFragment.newInstance(position, videos[position % videos.size])
    }

    override fun getCount() = Int.MAX_VALUE / 2
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_pager, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    Kohii[this].register(this)
        .attach(viewPager)

    viewPager.apply {
      adapter = VideoPagerAdapter(childFragmentManager, getApp().videos)
      pageMargin = -resources.getDimensionPixelSize(R.dimen.pager_horizontal_space_base)
    }
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    // TODO add this to README or somewhere useful.
    // In ViewPager (1), adding a PageTransformer may also call 'ViewPager#populate()' method
    // to init some pages (read setPageTransformer source code). This 'populate' method will call
    // 'populate(mCurItem)' where `mCurItem` is the position of current item. This value will always
    // be '0' before the ViewPager's saved state is restored.
    // Therefore: if our ViewPager is scrolled to the middle, then a configuration change happens,
    // setting a PageTransformer will ask the Adapter to create pages for position 0 and 1 without
    // any benefit (after the config change, we expect to see the page before the recreation).
    // Therefore, it is better to move the call to `setPageTransformer` to this callback because
    // it will be called regardless of there is saved state or not, and in case there is saved state,
    // it will be called after that state is restored.
    viewPager.apply {
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
