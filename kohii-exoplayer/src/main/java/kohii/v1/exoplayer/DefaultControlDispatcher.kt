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

package kohii.v1.exoplayer

import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import kohii.v1.core.Playback

internal class DefaultControlDispatcher(private val playback: Playback) : ControlDispatcher {

  override fun dispatchSeekTo(
    player: Player,
    windowIndex: Int,
    positionMs: Long
  ): Boolean {
    player.seekTo(windowIndex, positionMs)
    return true
  }

  override fun dispatchSetShuffleModeEnabled(
    player: Player,
    shuffleModeEnabled: Boolean
  ): Boolean {
    player.shuffleModeEnabled = shuffleModeEnabled
    return true
  }

  override fun dispatchSetPlayWhenReady(
    player: Player,
    playWhenReady: Boolean
  ): Boolean {
    val playable = playback.playable
    if (playable != null) {
      if (playWhenReady) playback.manager.play(playable)
      else playback.manager.pause(playable)
    }
    return true
  }

  override fun dispatchSetRepeatMode(
    player: Player,
    repeatMode: Int
  ): Boolean {
    player.repeatMode = repeatMode
    return true
  }

  override fun dispatchStop(
    player: Player,
    reset: Boolean
  ): Boolean {
    val playable = playback.playable
    if (playable != null) playback.manager.pause(playable)
    player.stop(reset)
    return true
  }
}
