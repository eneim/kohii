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

package kohii.v1.exo

import android.app.Activity
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.BridgeProvider
import kohii.v1.Cleanable
import kohii.v1.Kohii
import kohii.v1.OutputHolderManager
import kohii.v1.Playable
import kohii.v1.Playable.Config
import kohii.v1.PlayableCreator
import kohii.v1.Playback
import kohii.v1.Playback.PlayerAvailabilityCallback
import kohii.v1.PlaybackCreator
import kohii.v1.PlaybackManager
import kohii.v1.PlayerCreator
import kohii.v1.Target
import kohii.v1.ViewPlayback

/**
 * @author eneim (2019/03/14)
 *
 * Need to give the Config more power. Because Playable lives at global scope, so is instance of this class.
 * Responsibilities:
 * - Allow binding to specific type of Target.
 * - Judge from the target type to build correct Playable.
 */
class PlayerViewPlayableCreator(
  kohii: Kohii,
  private val playerCreator: PlayerCreator<ViewGroup, PlayerView>,
  bridgeCreator: (Kohii) -> BridgeProvider<PlayerView>
) : PlayableCreator<PlayerView>(kohii, PlayerView::class.java),
    PlaybackCreator<Any, PlayerView>,
    Cleanable, LifecycleObserver {

  internal constructor(
    kohii: Kohii
  ) : this(kohii, PlayerViewCreator(), { kohii.defaultBridgeProvider })

  init {
    kohii.cleanables.add(this)
    kohii.owners.forEach { it.value.activity.lifecycle.addObserver(this) }
  }

  private val outputHolderCache = ArrayMap<Activity, OutputHolderManager<ViewGroup, PlayerView>>()
  private val bridgeProvider = bridgeCreator.invoke(kohii)

  override fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Config
  ): Playable<PlayerView> {
    return PlayerViewPlayable(kohii, media, config, bridgeProvider, this)
  }

  override fun createPlayback(
    manager: PlaybackManager,
    target: Target<Any, PlayerView>,
    playable: Playable<PlayerView>,
    config: Playback.Config
  ): Playback<Any, PlayerView> {
    val container = target.requireContainer()
    val targetHost =
      manager.findHostForTarget(target.requireContainer()) ?: throw IllegalStateException(
          "This manager $this has no TargetHost that " +
              "accepts this target: $target. Kohii requires at least one."
      )

    val outputHolderManager = outputHolderCache.getOrPut(manager.parent.activity) {
      OutputHolderManager(2, playerCreator)
    }

    val result = when (container) {
      is PlayerView -> {
        @Suppress("UNCHECKED_CAST")
        ViewPlayback(
            kohii,
            playable,
            manager,
            targetHost,
            container,
            config
        ) as Playback<Any, PlayerView>
      }

      is ViewGroup -> {
        @Suppress("UNCHECKED_CAST")
        LazyViewPlayback(
            kohii,
            playable,
            manager,
            targetHost,
            target as Target<ViewGroup, PlayerView>,
            config,
            outputHolderManager
        ) as Playback<Any, PlayerView>
      }
      else -> throw IllegalArgumentException("")
    }

    if (playable is PlayerAvailabilityCallback) {
      result.availabilityCallback = playable
    }

    return result
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    if (owner is Activity) {
      outputHolderCache.remove(owner)
          ?.cleanUp()
    }
    owner.lifecycle.removeObserver(this)
  }

  override fun cleanUp() {
    bridgeProvider.cleanUp()
    outputHolderCache.onEach { it.value.cleanUp() }
        .clear()
  }
}
