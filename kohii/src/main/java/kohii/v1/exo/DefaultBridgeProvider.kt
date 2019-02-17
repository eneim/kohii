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

import kohii.v1.Bridge
import kohii.v1.BridgeProvider
import kohii.v1.Playable

/**
 * @author eneim (2018/10/28).
 */
class DefaultBridgeProvider(
  private val playerProvider: PlayerProvider,
  private val mediaSourceFactoryProvider: MediaSourceFactoryProvider
) : BridgeProvider {

  override fun provideBridge(builder: Playable.Builder): Bridge {
    return ExoBridge(
        builder.kohii,
        builder.media,
        playerProvider,
        mediaSourceFactoryProvider
    ).also {
      it.repeatMode = builder.repeatMode
      it.parameters = builder.playbackParameters
      it.playbackInfo = builder.playbackInfo
    }
  }
}
