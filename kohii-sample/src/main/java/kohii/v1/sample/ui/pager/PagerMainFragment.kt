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
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getDisplayPoint
import kohii.v1.sample.ui.overlay.data.Video
import kotlinx.android.synthetic.main.fragment_pager.viewPager
import okio.buffer
import okio.source

@Suppress("unused")
@Keep
class PagerMainFragment : BaseFragment() {

  companion object {
    fun newInstance() = PagerMainFragment()
    const val TAG = "kohii:Pager"
  }

  class VideoPagerAdapter(
    fm: FragmentManager?,
    private val videos: List<Video>
  ) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
      return PageFragment.newInstance(position, videos[position % videos.size])
    }

    override fun getCount() = Int.MAX_VALUE
  }

  private var items: List<Video>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val asset = requireActivity().assets
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val adapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    items = adapter.fromJson(asset.open("caminandes.json").source().buffer())
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
    this.viewPager.adapter = VideoPagerAdapter(childFragmentManager, items!!)

    this.viewPager.let {
      it.pageMargin = -resources.getDimensionPixelSize(R.dimen.pager_horizontal_space_base)
      val clientWidth =
        (requireActivity().getDisplayPoint().x - it.paddingStart - it.paddingEnd).toFloat()
      val offset = it.paddingStart / clientWidth
      it.setPageTransformer(false) { page, position ->
        val scale = (1f - Math.abs(position - offset) * 0.15f).coerceAtLeast(0.5f)
        page.scaleX = scale
        page.scaleY = scale
      }
    }
  }
}