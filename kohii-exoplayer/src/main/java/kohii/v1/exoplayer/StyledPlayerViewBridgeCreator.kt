/*
 * Copyright (c) 2023 Nam Nguyen, nam@ene.im
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

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import kohii.v1.core.Bridge
import kohii.v1.core.BridgeCreator
import kohii.v1.core.PlayerPool
import kohii.v1.media.Media

class StyledPlayerViewBridgeCreator(
  private val playerPool: PlayerPool<Player>,
  private val mediaSourceFactory: MediaSource.Factory
) : BridgeCreator<StyledPlayerView> {

  override fun createBridge(
    context: Context,
    media: Media
  ): Bridge<StyledPlayerView> {
    return StyledPlayerViewBridge(
      context,
      media,
      playerPool,
      mediaSourceFactory
    )
  }

  override fun cleanUp() {
    playerPool.clear()
  }
}
