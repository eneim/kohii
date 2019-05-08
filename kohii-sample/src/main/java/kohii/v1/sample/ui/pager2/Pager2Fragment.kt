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

package kohii.v1.sample.ui.pager2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kohii.v1.Kohii
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.data.Video
import kohii.v1.sample.ui.pager1.PageFragment
import kotlinx.android.synthetic.main.fragment_pager_2_horizontal.viewPager

// Only demo the case of using Fragment. Using ViewPager2 with normal View is equal to RecyclerView.
class Pager2Fragment : BaseFragment() {

  companion object {
    fun newInstance() = Pager2Fragment()
  }

  class VideoPagerAdapter(
    private val videos: List<Video>,
    fragment: Fragment
  ) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
      return Int.MAX_VALUE
    }

    override fun getItem(position: Int): Fragment {
      return PageFragment.newInstance(position, videos[position % videos.size])
    }
  }

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
    Kohii[this].register(this, viewPager)
    this.viewPager.adapter =
      VideoPagerAdapter((requireActivity().application as DemoApp).videos, this)
  }
}
