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

import android.util.Log
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
import kohii.v1.Playback.Config
import kohii.v1.Playback.PlayerAvailabilityCallback
import kohii.v1.PlaybackCreator
import kohii.v1.Target

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
  private val playbackCreator: PlaybackCreator<Any, PlayerView>
) : BasePlayable<PlayerView>(kohii, media, config, bridge), PlayerAvailabilityCallback {

  internal constructor(
    kohii: Kohii,
    media: Media,
    config: Playable.Config,
    bridgeProvider: BridgeProvider<PlayerView>,
    playbackCreator: PlaybackCreator<Any, PlayerView>
  ) : this(
      kohii, media, config, bridgeProvider.provideBridge(kohii, media, config),
      playbackCreator
  )

  @Suppress("UNCHECKED_CAST")
  override fun <TARGET : Any> bind(
    target: TARGET,
    config: Config,
    cb: ((Playback<TARGET, PlayerView>) -> Unit)?
  ) {
    val manager = kohii.findSuitableManager(target) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )

    val targetType = target.javaClass
    val boxedTarget =
      (when {
        // order is important.
        PlayerView::class.java.isAssignableFrom(targetType) ->
          PlayerViewTarget(target as PlayerView)
        ViewGroup::class.java.isAssignableFrom(targetType) ->
          PlayerViewTarget(target as ViewGroup)
        else -> throw IllegalArgumentException("Unsupported target type: $targetType")
      }) as Target<TARGET, PlayerView>

    val result = manager.performBindPlayable(
        this, boxedTarget, config, playbackCreator as PlaybackCreator<TARGET, PlayerView>
    )
    cb?.invoke(result)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <TARGET : Any> bind(
    target: Target<TARGET, PlayerView>,
    config: Config,
    cb: ((Playback<TARGET, PlayerView>) -> Unit)?
  ) {
    val actualTarget = target.requireContainer()
    val manager = kohii.findSuitableManager(actualTarget) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )

    Log.w("Kohii::X", "bind: $target, $manager")
    val result = manager.performBindPlayable(
        this, target, config, playbackCreator as PlaybackCreator<TARGET, PlayerView>
    )
    cb?.invoke(result)
  }

  override fun onPlayerActive(
    playback: Playback<*, *>,
    player: Any
  ) {
    if (player is PlayerView) {
      bridge.playerView = player
      val controller = playback.controller
      if (controller != null) {
        player.useController = true
        if (controller is ControlDispatcher) player.setControlDispatcher(controller)
      } else {
        player.useController = false
      }
    }
  }

  override fun onPlayerInActive(
    playback: Playback<*, *>,
    player: Any?
  ) {
    if (bridge.playerView === player) {
      bridge.playerView = null
      player?.setControlDispatcher(null) // TODO check this.
    }
  }
}
