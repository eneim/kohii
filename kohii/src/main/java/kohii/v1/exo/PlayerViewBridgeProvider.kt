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

import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.Bridge
import kohii.v1.BridgeProvider
import kohii.v1.Kohii
import kohii.v1.Playable

/**
 * @author eneim (2018/10/28).
 */
class PlayerViewBridgeProvider(
  kohii: Kohii,
  private val playerProvider: ExoPlayerProvider,
  private val mediaSourceFactoryProvider: MediaSourceFactoryProvider
) : BridgeProvider<PlayerView> {

  init {
    kohii.cleanables.add(this)
  }

  override fun provideBridge(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ): Bridge<PlayerView> {
    return PlayerViewBridge(
        kohii,
        media,
        playerProvider,
        mediaSourceFactoryProvider
    ).also {
      it.repeatMode = config.repeatMode
      it.parameters = config.playbackParameters
    }
  }

  override fun cleanUp() {
    this.playerProvider.cleanUp()
  }
}
