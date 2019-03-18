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
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.BasePlayable
import kohii.v1.Bridge
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.Playback.Config
import kohii.v1.PlaybackCreator
import kohii.v1.ViewPlayback

/**
 * @author eneim (2018/06/24).
 */
class PlayerViewPlayable internal constructor(
  kohii: Kohii,
  media: Media,
  config: Playable.Config,
  bridge: Bridge<PlayerView>
) : BasePlayable<PlayerView>(kohii, media, config, bridge) {

  internal constructor(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ) : this(kohii, media, config, kohii.bridgeProvider.provideBridge(kohii, media, config))

  override fun <TARGET : Any> bind(
    target: TARGET,
    config: Config,
    cb: ((Playback<TARGET, PlayerView>) -> Unit)?
  ) {
    val manager = kohii.findSuitableManager(target) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )

    val container by lazy {
      manager.findSuitableContainer(target) ?: throw IllegalStateException(
          "This manager $this has no Container that " +
              "accepts this target: $target. Kohii requires at least one."
      )
    }

    val creatorForPlayerView by lazy {
      object : PlaybackCreator<PlayerView, PlayerView> {
        override fun createPlayback(
          target: PlayerView,
          config: Config
        ): Playback<PlayerView, PlayerView> {
          return ViewPlayback(
              kohii, media, this@PlayerViewPlayable, manager, container, target, config
          )
        }
      }
    }

    val creatorForViewGroup by lazy {
      object : PlaybackCreator<ViewGroup, PlayerView> {
        override fun createPlayback(
          target: ViewGroup,
          config: Config
        ): Playback<ViewGroup, PlayerView> {
          return LazyViewPlayback(
              kohii, media, this@PlayerViewPlayable, manager, container, target, config,
              manager.parent.playerViewPool
          )
        }
      }
    }

    val targetType = target.javaClass
    @Suppress("UNCHECKED_CAST")
    val creator =
      (when {
        PlayerView::class.java.isAssignableFrom(targetType) -> creatorForPlayerView
        ViewGroup::class.java.isAssignableFrom(targetType) -> creatorForViewGroup
        else -> throw IllegalArgumentException("Unsupported target type: ")
      }) as PlaybackCreator<TARGET, PlayerView>

    Log.w("Kohii::X", "bind: $target, $manager")
    val result = manager.performBindPlayable(this, target, config, creator)
    cb?.invoke(result)
  }
}