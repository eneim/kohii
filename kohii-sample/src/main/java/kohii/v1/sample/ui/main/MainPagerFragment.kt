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

package kohii.v1.sample.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kohii.v1.core.Master.MemoryMode.LOW
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.getDisplayPoint
import kotlinx.android.synthetic.main.fragment_pager.viewPager
import kotlin.math.abs

class MainPagerFragment : BaseFragment() {

  companion object {
    fun newInstance() = MainPagerFragment()
  }

  class PagerAdapter(
    fm: FragmentManager,
    val items: List<DemoItem>
  ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
      return items[position].fragmentClass.newInstance()
    }

    override fun getCount(): Int {
      return items.size
    }
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
    Kohii[this].register(this, LOW)
        .attach(viewPager)

    this.viewPager.also {
      it.adapter = PagerAdapter(childFragmentManager, getApp().demoItems)
      it.pageMargin = -resources.getDimensionPixelSize(R.dimen.pager_horizontal_space_base)
      val clientWidth =
        (requireActivity().getDisplayPoint().x - it.paddingStart - it.paddingEnd).toFloat()
      val offset = it.paddingStart / clientWidth
      it.setPageTransformer(false) { page, position ->
        val scale = (1f - abs(position - offset) * 0.15f).coerceAtLeast(0.5f)
        page.scaleX = scale
        page.scaleY = scale
      }
    }
  }
}
