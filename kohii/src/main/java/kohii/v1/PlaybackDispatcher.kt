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
import android.util.Log

/**
 *
 * @since 2018/12/26
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

  internal fun onAttached() {
    if (handler == null) handler = Handler(Looper.getMainLooper(), this)
  }

  internal fun onDetached() {
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
    Log.d("Kohii::X", "play: ${playback.tag}")
    playback.playable.ensurePreparation()

    val controller = playback.controller
    if (controller != null) {
      if (kohii.manualPlayables.isNotEmpty() /* has playback started by User */ &&
          !kohii.manualPlayables.containsKey(playback.playable) /* but not this playback */) {
        justPause(playback)
        return
      }

      val state = kohii.manualPlayableState[playback.playable]
      if (state != null) { // playback is started or paused by User, we do not override that action.
        if (state != true) justPause(playback)
        else justPlay(playback)
        return
      } else {
        // no history of User action, let's calculate next action by System
        if (controller.startBySystem()) {
          // should start by System.
          kohii.manualPlayableState[playback.playable] = true
          if (!controller.pauseBySystem()) {
            kohii.manualPlayables[playback.playable] = Kohii.PRESENT
          }
          justPlay(playback)
        }
      }
    } else {
      justPlay(playback)
    }
  }

  internal fun pause(playback: Playback<*>) {
    Log.i("Kohii::X", "pause: ${playback.tag}")
    // There is PlaybackManagerGroup whose selection is not empty.
    if (playback.kohii.groups.filter { it.value.selection.isNotEmpty() }.isNotEmpty()) {
      justPause(playback)
      return
    }

    val controller = playback.controller
    if (controller != null) {
      if (kohii.manualPlayables.isNotEmpty() /* has playback started by User */ &&
          !kohii.manualPlayables.containsKey(playback.playable) /* but not this playback */) {
        justPause(playback)
        return
      }

      val state = kohii.manualPlayableState[playback.playable]
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
