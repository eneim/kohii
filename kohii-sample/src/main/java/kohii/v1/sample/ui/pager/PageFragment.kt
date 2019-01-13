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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.ui.pager.data.Video
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView

class PageFragment : BaseFragment() {

  companion object {
    private const val pageVideoKey = "kohii:demo:page:video"
    private const val pageTagKey = "kohii:demo:page:tag"

    fun newInstance() = PageFragment().also {
      it.arguments = Bundle()
    }

    fun newInstance(
      position: Int,
      video: Video
    ) = newInstance().also {
      it.arguments?.run {
        putParcelable(pageVideoKey, video)
        putInt(pageTagKey, position)
      }
    }
  }

  val video by lazy {
    val video = arguments?.getParcelable(pageVideoKey) as Video
    val item = video.playlist.first()
        .sources.first()
    item
  }

  var landscape: Boolean = false
  var playback: Playback<PlayerView>? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    landscape = requireActivity().isLandscape()
    val viewRes =
      if (landscape) R.layout.fragment_pager_page_land else R.layout.fragment_pager_page_port
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val pagePos = arguments?.getInt(pageTagKey) ?: -1
    val videoTag = "${video.file}::$pagePos"
    playback = Kohii[this].setUp(video.file)
        .copy(repeatMode = Player.REPEAT_MODE_ONE, prefetch = landscape)
        .copy(tag = videoTag)
        .asPlayable()
        .bind(playerView)
        .also { it.observe(viewLifecycleOwner) }
  }
}