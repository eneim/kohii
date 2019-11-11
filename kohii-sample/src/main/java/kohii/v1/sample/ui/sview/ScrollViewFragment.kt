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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Master
import kohii.core.Playback
import kohii.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.player.InitData
import kotlinx.android.synthetic.main.fragment_scroll_view.playerContainer
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView
import kotlinx.android.synthetic.main.fragment_scroll_view.scrollView

@Keep
class ScrollViewFragment : BaseFragment(), PlayerDialogFragment.Callback {

  companion object {
    const val videoUrl =
      // http://www.caminandes.com/download/03_caminandes_llamigos_1080p.mp4
      "https://content.jwplatform.com/manifests/146UwF4L.m3u8" // Big Buck Bunny

    fun newInstance() = ScrollViewFragment().also {
      it.arguments = Bundle()
    }
  }

  private val videoTag by lazy { "${javaClass.canonicalName}::$videoUrl" }

  private lateinit var kohii: Master
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
    kohii = Master[this]
    kohii.register(this)
        .attach(scrollView)

    val rebinder = kohii.setUp(videoUrl)
        .with {
          tag = videoTag
          repeatMode = Player.REPEAT_MODE_ONE
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

  override fun onDialogInActive(rebinder: Rebinder<PlayerView>) {
    rebinder.bind(kohii, playerView) {
      playback = it
    }
  }

  // END: PlayerDialogFragment.Callback
}
