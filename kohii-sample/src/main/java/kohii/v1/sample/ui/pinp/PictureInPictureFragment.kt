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
import com.google.android.exoplayer2.Player
import kohii.v1.core.Playback
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.FragmentPipBinding

@RequiresApi(VERSION_CODES.O)
class PictureInPictureFragment : BaseFragment(), Playback.StateListener {

  companion object {
    fun newInstance() = PictureInPictureFragment()
  }

  private lateinit var binding: FragmentPipBinding
  private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()
  private var playback: Playback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding: FragmentPipBinding = FragmentPipBinding.inflate(inflater, container, false)
    this.binding = binding
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.pipButton.setOnClickListener { minimize() }
    binding.playerContainer.setAspectRatio(16 / 9F)
    val kohii = Kohii[this]
    kohii.register(this)
      .addBucket(binding.playerContainer)

    kohii.setUp(assetVideoUri) {
      tag = "${javaClass.name}::$videoUrl"
      repeatMode = Player.REPEAT_MODE_ONE
    }
      .bind(binding.playerView) {
        it.addStateListener(this@PictureInPictureFragment)
        playback = it
      }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  internal fun minimize() {
    playback?.let {
      mPictureInPictureParamsBuilder
        .setAspectRatio(Rational(binding.playerContainer.width, binding.playerContainer.height))
        .setSourceRectHint(it.containerRect)

      requireActivity().enterPictureInPictureMode(mPictureInPictureParamsBuilder.build())
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playback?.removeStateListener(this)
    playback = null
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    binding.scrollView.isVisible = !isInPictureInPictureMode
  }

  override fun onVideoSizeChanged(
    playback: Playback,
    width: Int,
    height: Int,
    unAppliedRotationDegrees: Int,
    pixelWidthHeightRatio: Float
  ) {
    binding.playerContainer.setAspectRatio(width / height.toFloat())
  }
}
