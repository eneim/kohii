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

package kohii.v1

import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 *
 * @since 2018/12/26
 *
 * Dispatch the play/pause action to [Playback].
 * As [Playback] supports deplayed start, this class correctly dispatch that start action at proper timing.
 * It also synchronize the play/pause behavior with [Kohii]'s global states like manual playback.
 *
 */
class PlaybackDispatcher(val kohii: Kohii) : Handler.Callback {
  companion object {
    private const val MSG_PLAY = 1234
  }

  override fun handleMessage(msg: Message?): Boolean {
    if (msg?.what == MSG_PLAY && msg.obj is Playback<*>) {
      (msg.obj as Playback<*>).playInternal()
    }
    return true
  }

  private var handler: Handler? = null

  internal fun onStart() {
    if (handler == null) handler = Handler(Looper.getMainLooper(), this)
  }

  internal fun onStop() {
    handler?.removeCallbacksAndMessages(null)
    handler = null
  }

  private fun justPlay(playback: Playback<*>) {
    handler?.also {
      val delay = playback.config.delay
      it.removeMessages(MSG_PLAY, playback)
      when {
        delay <= Playback.DELAY_INFINITE -> {
          // ignored
        }
        delay == 0 -> playback.playInternal()
        else -> it.sendMessageDelayed(it.obtainMessage(MSG_PLAY, playback), delay.toLong())
      }
    }
  }

  private fun justPause(playback: Playback<*>) {
    handler?.removeMessages(MSG_PLAY, playback)
    playback.pauseInternal()
  }

  internal fun play(playback: Playback<*>) {
    playback.playable.ensurePreparation()

    val controller = playback.controller
    if (controller != null) {
      if (kohii.manualPlayables.isNotEmpty() /* has playback started by User */ &&
          !kohii.manualPlayables.contains(playback.playable) /* but not this playback */) {
        justPause(playback) // has lower priority --> pause if need.
        return
      }

      val state = kohii.manualPlayableRecord[playback.playable]
      if (state != null) { // playback is started or paused by User --> we do not override that action.
        if (state != true) justPause(playback) // User paused this playback, so ensure that.
        else justPlay(playback) // User played this playback, so ensure that.
        return
      } else {
        // no history of User action, let's determine next action by System
        if (controller.startBySystem()) {
          // should start by System.
          kohii.manualPlayableRecord[playback.playable] = true
          if (!controller.pauseBySystem()) {
            // mark a Playable as being played by User --> System will not pause it.
            kohii.manualPlayables.add(playback.playable)
          }
          justPlay(playback)
        }
      }
    } else {
      justPlay(playback)
    }
  }

  internal fun pause(playback: Playback<*>) {
    // There is PlaybackManagerGroup whose selection is not empty.
    if (playback.kohii.groups.filter { it.value.selection.isNotEmpty() }.isNotEmpty()) {
      justPause(playback)
      return
    }

    val controller = playback.controller
    if (controller != null) {
      if (kohii.manualPlayables.isNotEmpty() /* has playback started by User */ &&
          !kohii.manualPlayables.contains(playback.playable) /* but not this playback */) {
        justPause(playback)
        return
      }

      val state = kohii.manualPlayableRecord[playback.playable]
      if (state != null && state == false) {
        justPause(playback)
      } else if (controller.pauseBySystem()) {
        justPause(playback)
      }
    } else {
      justPause(playback)
    }
  }

  internal fun onPlaybackRemoved(playback: Playback<*>) {
    handler?.removeMessages(MSG_PLAY, playback)
  }
}
