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

package kohii.v1.sample.ui.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.sview.ScrollViewFragment
import kotlinx.android.synthetic.main.fragment_pager.viewPager

class PagerMainFragment : BaseFragment() {

  companion object {
    fun newInstance() = PagerMainFragment()
  }

  class VideoPagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
      return ScrollViewFragment.newInstance("Page: $position")
    }

    override fun getCount() = Int.MAX_VALUE
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
    this.viewPager.let {
      it.adapter = VideoPagerAdapter(childFragmentManager)
      it.pageMargin = resources.getDimensionPixelOffset(R.dimen.pager_horizontal_space)
    }
  }
}