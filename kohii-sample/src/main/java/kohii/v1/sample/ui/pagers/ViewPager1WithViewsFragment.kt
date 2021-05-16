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
import androidx.viewpager.widget.PagerAdapter
import com.google.android.exoplayer2.Player
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.getDisplayPoint
import kohii.v1.sample.common.inflateView
import kohii.v1.sample.data.Video
import kohii.v1.sample.ui.main.DemoItem
import kotlinx.android.synthetic.main.fragment_pager.viewPager
import kotlinx.android.synthetic.main.widget_video_container.view.videoFrame
import kotlin.math.abs

// ViewPager whose pages are Views
class ViewPager1WithViewsFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = ViewPager1WithViewsFragment()
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

    override fun getCount() = Int.MAX_VALUE / 2

    override fun instantiateItem(
      container: ViewGroup,
      position: Int
    ): Any {
      // Normal creation
      val view = container.inflateView(R.layout.widget_video_container)
      container.addView(view)
      // Now bind the content
      val video = videos[position % videos.size].playlist.first()
          .sources.first()
      val itemTag = "$javaClass::$position::${video.file}"
      kohii.setUp(video.file) {
        tag = itemTag
        delay = 500
        preload = true
        repeatMode = Player.REPEAT_MODE_ONE
      }
          .bind(view.videoFrame)
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

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

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
    val kohii = Kohii[this]
    kohii.register(this)
        .addBucket(viewPager)

    viewPager.apply {
      adapter = PagerPagesAdapter(kohii, getApp().videos)
      setPadding(0)
    }
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
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
