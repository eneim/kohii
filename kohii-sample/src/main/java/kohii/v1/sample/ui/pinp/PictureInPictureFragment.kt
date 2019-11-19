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
import kohii.v1.core.Common
import kohii.v1.core.Playback
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_pip.pipButton
import kotlinx.android.synthetic.main.fragment_pip.playerContainer
import kotlinx.android.synthetic.main.fragment_pip.playerView
import kotlinx.android.synthetic.main.fragment_pip.scrollView

@RequiresApi(VERSION_CODES.O)
class PictureInPictureFragment : BaseFragment(), Playback.PlaybackListener {

  companion object {
    fun newInstance() = PictureInPictureFragment()
  }

  private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()
  private var playback: Playback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_pip, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    pipButton.setOnClickListener { minimize() }
    playerContainer.setAspectRatio(16 / 9F)
    val kohii = Kohii[this]
    kohii.register(this)
        .attach(playerContainer)

    kohii.setUp(assetVideoUri) {
      tag = "${javaClass.name}::$videoUrl"
      repeatMode = Common.REPEAT_MODE_ONE
    }
        .bind(playerView) {
          it.addPlaybackListener(this@PictureInPictureFragment)
          playback = it
        }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  internal fun minimize() {
    playback?.let {
      mPictureInPictureParamsBuilder
          .setAspectRatio(Rational(playerContainer.width, playerContainer.height))
          .setSourceRectHint(it.containerRect)

      requireActivity().enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playback?.removePlaybackListener(this)
    playback = null
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    scrollView.isVisible = !isInPictureInPictureMode
  }

  override fun onVideoSizeChanged(
    playback: Playback,
    width: Int,
    height: Int,
    unAppliedRotationDegrees: Int,
    pixelWidthHeightRatio: Float
  ) {
    playerContainer.setAspectRatio(width / height.toFloat())
  }
}
