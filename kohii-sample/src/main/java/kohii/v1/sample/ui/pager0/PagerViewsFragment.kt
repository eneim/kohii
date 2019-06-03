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

package kohii.v1.sample.ui.pager0

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.ViewTarget
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.data.Video
import kotlinx.android.synthetic.main.fragment_pager.viewPager
import kotlinx.android.synthetic.main.widget_video_container.view.videoFrame

// Using ViewPager with Views instead of Fragments
class PagerViewsFragment : BaseFragment() {

  companion object {
    fun newInstance() = PagerViewsFragment()
  }

  class PagerPagesAdapter(
    val kohii: Kohii,
    private val videos: List<Video>
  ) : PagerAdapter() {

    override fun isViewFromObject(
      view: View,
      `object`: Any
    ): Boolean {
      return view === `object`
    }

    override fun getCount(): Int {
      return Int.MAX_VALUE
    }

    override fun instantiateItem(
      container: ViewGroup,
      position: Int
    ): Any {
      val view = LayoutInflater.from(container.context)
          .inflate(R.layout.widget_video_container, container, false)
      container.addView(view)
      val video = videos[position % videos.size].playlist.first()
          .sources.first()
      val itemTag = "$javaClass::$position::${video.file}"
      kohii.setUp(video.file)
          .with {
            tag = itemTag
            repeatMode = Playable.REPEAT_MODE_ONE
          }
          .bind(ViewTarget(view.videoFrame))
      return view
    }

    override fun destroyItem(
      container: ViewGroup,
      position: Int,
      `object`: Any
    ) {
      if (`object` is View) {
        container.removeView(`object`)
      }
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
    val kohii = Kohii[this].also { it.register(this, viewPager) }

    this.viewPager.adapter =
      PagerPagesAdapter(kohii, (requireActivity().application as DemoApp).videos)
  }
}
