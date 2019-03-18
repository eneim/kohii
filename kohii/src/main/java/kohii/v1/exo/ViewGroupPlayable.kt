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

class ViewGroupPlayable(
  kohii: Kohii,
  media: Media,
  config: Playable.Config,
  bridge: Bridge<PlayerView>
) : BasePlayable<ViewGroup, PlayerView>(kohii, media, config, bridge) {

  internal constructor(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ) : this(kohii, media, config, kohii.bridgeProvider.provideBridge(kohii, media, config))

  override fun bind(
    target: ViewGroup,
    config: Playback.Config,
    cb: ((Playback<ViewGroup>) -> Unit)?
  ) {
    val manager = kohii.findSuitableManager(target) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )
    Log.w("Kohii::X", "bind: $target, $manager")
    val result = manager.performBindPlayable(this, target, config,
        object : PlaybackCreator<ViewGroup> {
          override fun createPlayback(
            target: ViewGroup,
            config: Config
          ): Playback<ViewGroup> {
            val container = manager.findSuitableContainer(target)
                ?: throw IllegalStateException(
                    "This manager $this has no Container that " +
                        "accepts this target: $target. Kohii requires at least one."
                )
            return ViewGroupPlayback(
                kohii, media, this@ViewGroupPlayable, manager, container, target, config,
                manager.parent.playerViewPool
            )
          }
        })
    cb?.invoke(result)
  }
}