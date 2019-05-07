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

package kohii.v1.exo

import android.view.ViewGroup
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.BasePlayable
import kohii.v1.Bridge
import kohii.v1.BridgeProvider
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.PlaybackCreator
import kohii.v1.Target
import kohii.v1.ViewTarget

/**
 * @author eneim (2018/06/24).
 *
 * [Playable] that accepts a [PlayerView] as playback presenter.
 */
internal class PlayerViewPlayable internal constructor(
  kohii: Kohii,
  media: Media,
  config: Playable.Config,
  bridge: Bridge<PlayerView>,
  playbackCreator: PlaybackCreator<PlayerView>
) : BasePlayable<PlayerView>(kohii, media, config, bridge, playbackCreator) {

  internal constructor(
    kohii: Kohii,
    media: Media,
    config: Playable.Config,
    bridgeProvider: BridgeProvider<PlayerView>,
    playbackCreator: PlaybackCreator<PlayerView>
  ) : this(
      kohii, media, config, bridgeProvider.provideBridge(kohii, media, config),
      playbackCreator
  )

  override fun <CONTAINER : Any> createBoxedTarget(target: CONTAINER): Target<CONTAINER, PlayerView> {
    val targetType = target.javaClass
    @Suppress("UNCHECKED_CAST")
    if (ViewGroup::class.java.isAssignableFrom(targetType))
      return ViewTarget<ViewGroup, PlayerView>(target as ViewGroup) as Target<CONTAINER, PlayerView>
    else throw IllegalArgumentException("Unsupported target type: $targetType")
  }

  override fun onPlayerActive(
    playback: Playback<PlayerView>,
    player: PlayerView
  ) {
    bridge.playerView = player
    val controller = playback.controller
    if (controller != null) {
      player.useController = true
      if (controller is ControlDispatcher) player.setControlDispatcher(controller)
    } else {
      // Force PlayerView to not use Controller.
      player.useController = false
    }
  }

  override fun onPlayerInActive(
    playback: Playback<PlayerView>,
    player: PlayerView?
  ) {
    if (bridge.playerView === player) {
      bridge.playerView = null
    }
  }
}
