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
import androidx.core.view.doOnLayout
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.core.Master
import kohii.core.Playback
import kohii.v1.Prioritized
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.data.Sources
import kohii.v1.sample.data.Video
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView

class PageFragment : BaseFragment(), Prioritized {

  companion object {
    private const val pageVideoKey = "kohii:demo:pager:video"
    private const val pageTagKey = "kohii:demo:pager:tag"

    fun newInstance() = PageFragment().also {
      it.arguments = Bundle()
    }

    fun newInstance(
      position: Int,
      video: Video
    ) = newInstance().also {
      it.arguments?.run {
        putParcelable(
            pageVideoKey, video
        )
        putInt(pageTagKey, position)
      }
    }
  }

  val video: Sources by lazy {
    val video = arguments?.getParcelable<Video>(
        pageVideoKey
    )!!
    val item = video.playlist.first()
        .sources.first()
    item
  }

  private var landscape: Boolean = false
  private var playback: Playback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    landscape = requireActivity().isLandscape()
    val viewRes =
      if (landscape) {
        R.layout.fragment_pager_page_land
      } else {
        R.layout.fragment_pager_page_port
      }
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Master[this]
    kohii.register(this)
        .attach(view.findViewById(R.id.content) as View)

    val pagePos = requireArguments().getInt(
        pageTagKey
    )
    val videoTag = "${javaClass.canonicalName}::${video.file}::$pagePos"
    kohii.setUp(video.file)
        .with {
          tag = videoTag
          repeatMode = Player.REPEAT_MODE_ONE
          preload = true
        }
        .bind(playerView) { playback = it }

    view.doOnLayout {
      val container = view.findViewById<View>(R.id.playerContainer)
      // [1] Update resize mode based on Window size.
      (container as? AspectRatioFrameLayout)?.also { ctn ->
        if (it.width * 9 >= it.height * 16) {
          // if (it.width * it.height >= it.height * it.width) { // how about this?
          ctn.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        } else {
          ctn.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
      }
    }
  }
}
