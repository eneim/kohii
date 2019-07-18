/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.sview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.player.InitData
import kohii.v1.sample.ui.player.PlayerDialogFragment
import kotlinx.android.synthetic.main.fragment_scroll_view.playerContainer
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView
import kotlinx.android.synthetic.main.fragment_scroll_view.scrollView

@Keep
class ScrollViewFragment : BaseFragment(), PlayerDialogFragment.Callback {

  companion object {
    const val videoUrl =
      // http://www.caminandes.com/download/03_caminandes_llamigos_1080p.mp4
      "https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8" // Big Buck Bunny

    fun newInstance() = ScrollViewFragment().also {
      it.arguments = Bundle()
    }
  }

  private val videoTag by lazy { "${javaClass.canonicalName}::$videoUrl" }

  private lateinit var kohii: Kohii
  private var playback: Playback<*>? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val viewRes = R.layout.fragment_scroll_view
    return inflater.inflate(viewRes, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    kohii = Kohii[this].also { it.register(this, this.scrollView) }
    val rebinder = kohii.setUp(videoUrl)
        .with {
          tag = videoTag
          repeatMode = Playable.REPEAT_MODE_ONE
        }
        .bind(playerView) { playback = it }

    playerContainer.setOnClickListener {
      rebinder?.also {
        PlayerDialogFragment.newInstance(rebinder, InitData(tag = videoTag, aspectRatio = 16 / 9f))
            .show(childFragmentManager, videoTag)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playerContainer.setOnClickListener(null)
  }

  // BEGIN: PlayerDialogFragment.Callback

  override fun onDialogActive() {
  }

  override fun onDialogInActive(rebinder: Rebinder) {
    rebinder.rebind(kohii, playerView) {
      playback = it
    }
  }

  // END: PlayerDialogFragment.Callback
}
