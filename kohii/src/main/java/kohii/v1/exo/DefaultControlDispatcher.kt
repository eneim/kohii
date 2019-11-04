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

import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Playback.Controller
import kohii.v1.PlaybackManager

open class DefaultControlDispatcher(
  private val manager: PlaybackManager,
  private val playerView: PlayerView,
  private val kohiiCanStart: Boolean = true,
  private val kohiiCanPause: Boolean = true
) : ControlDispatcher, Controller {

  override fun kohiiCanPause() = this.kohiiCanPause

  override fun kohiiCanStart() = this.kohiiCanStart

  override fun dispatchSeekTo(
    player: Player?,
    windowIndex: Int,
    positionMs: Long
  ): Boolean {
    return manager.findPlaybackForRenderer(playerView)?.let {
      it.seekTo(positionMs)
      true
    } ?: false
  }

  override fun dispatchSetShuffleModeEnabled(
    player: Player?,
    shuffleModeEnabled: Boolean
  ): Boolean {
    return false
  }

  override fun dispatchSetPlayWhenReady(
    player: Player?,
    playWhenReady: Boolean
  ): Boolean {
    return manager.findPlaybackForRenderer(playerView)?.let {
      if (playWhenReady) manager.play(it)
      else manager.pause(it)
      true
    } ?: false
  }

  override fun dispatchSetRepeatMode(
    player: Player?,
    repeatMode: Int
  ): Boolean {
    return manager.findPlaybackForRenderer(playerView)?.let {
      it.repeatMode = repeatMode
      true
    } ?: false
  }

  // Currently dispatched by background playback controller.
  override fun dispatchStop(
    player: Player?,
    reset: Boolean
  ): Boolean {
    return false
  }
}
