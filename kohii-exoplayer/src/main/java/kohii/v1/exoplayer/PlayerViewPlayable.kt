/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.exoplayer

import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.AbstractPlayable
import kohii.v1.core.Bridge
import kohii.v1.core.Master
import kohii.v1.core.Playback
import kohii.v1.media.Media

class PlayerViewPlayable(
  master: Master,
  media: Media,
  config: Config,
  bridge: Bridge<PlayerView>
) : AbstractPlayable<PlayerView>(master, media, config, bridge) {

  private val defaultControlDispatcher = DefaultControlDispatcher()

  override var renderer: Any?
    get() = bridge.renderer
    set(value) {
      require(value is PlayerView?)
      bridge.renderer = value
    }

  override fun onRendererAttached(playback: Playback, renderer: Any?) {
    val controller = playback.config.controller
    if (renderer is PlayerView) {
      if (controller is ControlDispatcher) {
        renderer.setControlDispatcher(controller)
        renderer.useController = true
      } else {
        renderer.setControlDispatcher(defaultControlDispatcher)
        renderer.useController = false
      }
    }
    super.onRendererAttached(playback, renderer)
    if (renderer is PlayerView && renderer.useController && controller == null) {
      throw IllegalStateException(
          "To enable `useController`, Playback $playback must have a non-null Playback.Controller."
      )
    }
  }

  override fun onRendererDetached(playback: Playback, renderer: Any?) {
    if (renderer is PlayerView) {
      renderer.setControlDispatcher(defaultControlDispatcher)
      renderer.useController = false
    }
    super.onRendererDetached(playback, renderer)
  }
}
