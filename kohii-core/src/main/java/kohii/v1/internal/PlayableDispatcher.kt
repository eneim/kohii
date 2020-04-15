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

package kohii.v1.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import kohii.v1.core.Common
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.logWarn

internal class PlayableDispatcher(val master: Master) : Handler.Callback {

  companion object {
    private const val MSG_PLAY = 100
  }

  override fun handleMessage(msg: Message): Boolean {
    if (msg.what == MSG_PLAY) (msg.obj as Playable).onPlay()
    return true
  }

  private val handler = Handler(Looper.getMainLooper(), this)

  internal fun onStart() {
    // Do nothing
  }

  internal fun onStop() {
    handler.removeCallbacksAndMessages(null)
  }

  internal fun play(playable: Playable) {
    "Dispatcher#play: $playable".logWarn()
    val manuallyStartedPlayable = master.manuallyStartedPlayable.get()
    // Has manual controller
    if (manuallyStartedPlayable != null /* has Playable started by client */
        && manuallyStartedPlayable.isPlaying() /* the Playable is playing */
        && manuallyStartedPlayable !== playable /* but not this one */
    ) {
      // Pause due to lower priority.
      justPause(playable)
      return
    }

    playable.onReady()
    if (!master.plannedManualPlayables.contains(playable.tag)) {
      justPlay(playable)
    } else {
      val nextAction = master.playablesPendingActions[playable.tag]
      if (nextAction != null) { // A flag was set somewhere by User/Client reaction.
        if (nextAction == Common.PLAY) justPlay(playable)
        else justPause(playable)
      } else {
        val controller = requireNotNull(playable.playback?.config?.controller)
        // No history of User action, let's determine next action by ourselves
        if (controller.kohiiCanStart()) {
          justPlay(playable)
        }
      }
    }
  }

  internal fun pause(playable: Playable) {
    "Dispatcher#pause: $playable".logWarn()
    val manuallyStartedPlayable = master.manuallyStartedPlayable.get()
    // Has manual controller
    if (manuallyStartedPlayable != null /* has Playable started by client */
        && manuallyStartedPlayable.isPlaying() /* the Playable is playing */
        && manuallyStartedPlayable !== playable /* but not this one */
    ) {
      justPause(playable)
      return
    }

    if (master.groups.find { it.selection.isNotEmpty() } != null) {
      justPause(playable)
      return
    }

    if (!master.plannedManualPlayables.contains(playable.tag)) {
      justPause(playable)
    } else {
      val controller = requireNotNull(playable.playback?.config?.controller)
      val nextAction = master.playablesPendingActions[playable.tag]
      if (nextAction != null && nextAction == Common.PAUSE) {
        justPause(playable)
      } else {
        if (controller.kohiiCanPause()) {
          justPause(playable)
        }
      }
    }
  }

  private fun justPlay(playable: Playable) {
    val delay = playable.playback?.config?.delay ?: 0
    handler.removeMessages(MSG_PLAY, playable)
    if (delay > 0) {
      val msg = handler.obtainMessage(MSG_PLAY, playable)
      handler.sendMessageDelayed(msg, delay.toLong())
    } else {
      playable.onPlay()
    }
  }

  private fun justPause(playable: Playable) {
    handler.removeMessages(MSG_PLAY, playable)
    playable.onPause()
  }
}
