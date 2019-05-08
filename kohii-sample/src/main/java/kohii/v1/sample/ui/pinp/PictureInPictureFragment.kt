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

package kohii.v1.sample.ui.pinp

import android.app.PictureInPictureParams
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import kohii.v1.Kohii
import kohii.v1.Playback.Controller
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_pip.pipButton
import kotlinx.android.synthetic.main.fragment_pip.playerContainer
import kotlinx.android.synthetic.main.fragment_pip.playerView
import kotlinx.android.synthetic.main.fragment_pip.scrollView

@RequiresApi(VERSION_CODES.O)
class PictureInPictureFragment : BaseFragment() {

  companion object {
    val videoUrl = "https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8"
    fun newInstance() = PictureInPictureFragment()
  }

  private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_pip, container, false)
  }

  lateinit var kohii: Kohii

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    pipButton.setOnClickListener { minimize() }

    kohii = Kohii[this].also { it.register(this, playerContainer) }
    kohii.setUp(videoUrl)
        .with {
          tag = "${javaClass.name}::videoUrl"
          controller = object : Controller {
            override fun pauseBySystem(): Boolean {
              return true
            }
          }
        }
        .bind(playerView)
  }

  private fun minimize() {
    mPictureInPictureParamsBuilder.setAspectRatio(
        Rational(playerContainer.width, playerContainer.height)
    )
    scrollView.isVisible = false
    requireActivity().enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    scrollView.isVisible = !isInPictureInPictureMode
    playerView.useController = !isInPictureInPictureMode
  }
}
