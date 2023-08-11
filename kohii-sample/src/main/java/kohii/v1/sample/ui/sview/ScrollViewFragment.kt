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
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.InitData
import kohii.v1.sample.databinding.FragmentScrollViewBinding
import kohii.v1.sample.ui.main.DemoItem

@Keep
class ScrollViewFragment : BaseFragment(), PlayerDialogFragment.Callback, DemoContainer {

  companion object {
    const val videoUrl =
      // http://www.caminandes.com/download/03_caminandes_llamigos_1080p.mp4
      // "https://content.jwplatform.com/manifests/146UwF4L.m3u8" // Big Buck Bunny
      assetVideoUri

    fun newInstance() = ScrollViewFragment().also {
      it.arguments = Bundle()
    }
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  private val videoTag by lazy { "${javaClass.canonicalName}::$videoUrl" }

  private lateinit var kohii: Kohii
  private lateinit var binding: FragmentScrollViewBinding
  private var playback: Playback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentScrollViewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    kohii.register(this)
      .addBucket(binding.scrollView)

    val rebinder = kohii.setUp(videoUrl) {
      tag = videoTag
      repeatMode = Player.REPEAT_MODE_ONE
    }
      .bind(binding.playerView) { playback = it }

    binding.playerContainer.setOnClickListener {
      rebinder?.also {
        PlayerDialogFragment.newInstance(rebinder, InitData(tag = videoTag, aspectRatio = 16 / 9f))
          .show(childFragmentManager, videoTag)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.playerContainer.setOnClickListener(null)
  }

  // BEGIN: PlayerDialogFragment.Callback

  override fun onDialogActive() {
  }

  override fun onDialogInActive(rebinder: Rebinder) {
    rebinder.bind(kohii, binding.playerView) {
      playback = it
    }
  }

  // END: PlayerDialogFragment.Callback
}
