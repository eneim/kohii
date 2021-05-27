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

package kohii.v1.dev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Manager
import kohii.v1.core.Manager.OnSelectionListener
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Controller
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.ActivityDevScrollviewBinding

class DevScrollViewFragment : BaseFragment(), OnSelectionListener {

  lateinit var binding: ActivityDevScrollviewBinding

  lateinit var kohii: Kohii
  lateinit var manager: Manager

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = ActivityDevScrollviewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    manager = kohii.register(this)
        .addBucket(binding.scrollView)

    kohii.setUp(DemoApp.assetVideoUri) {
      tag = "player::0"
      repeatMode = Player.REPEAT_MODE_ONE
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true

        override fun kohiiCanPause(): Boolean = true

        override fun setupRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            val controller = kohii.createControlDispatcher(playback)
            renderer.setControlDispatcher(controller)
            renderer.useController = true
            renderer.tag = controller
          }
        }

        override fun teardownRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            val tag = renderer.tag
            if (tag is ControlDispatcher) renderer.tag = null
          }
        }
      }
    }
        .bind(binding.playerView1)

    kohii.setUp("https://content.jwplatform.com/manifests/Cl6EVHgQ.m3u8") {
      tag = "player::1"
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true

        override fun kohiiCanPause(): Boolean = true

        override fun setupRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            val controller = kohii.createControlDispatcher(playback)
            renderer.setControlDispatcher(controller)
            renderer.useController = true
            renderer.tag = controller
          }
        }

        override fun teardownRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            val tag = renderer.tag
            if (tag is ControlDispatcher) renderer.tag = null
          }
        }
      }
    }
        .bind(binding.playerView2)
  }

  override fun onSelection(selection: Collection<Playback>) {
    val playback = selection.firstOrNull()
    if (playback != null) {
      binding.controlView.showTimeoutMs = -1 // non-positive so it will never hide
      binding.controlView.show()
      val container = playback.container
      if (container is PlayerView) {
        container.useController = false // if you want to only use the global controller.
        binding.controlView.player = container.player
        val controller = container.tag
        if (controller is ControlDispatcher) {
          binding.controlView.setControlDispatcher(controller)
        }
      }
    } else {
      binding.controlView.setControlDispatcher(DefaultControlDispatcher())
      binding.controlView.player = null
      binding.controlView.hide()
    }
  }
}
