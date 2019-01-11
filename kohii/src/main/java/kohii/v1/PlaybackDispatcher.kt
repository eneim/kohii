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

/** @since 2018/12/26 */

class PlaybackDispatcher : Handler.Callback {
  companion object {
    private const val MSG_PLAY = 1234
  }

  override fun handleMessage(msg: Message?): Boolean {
    if (msg?.what == MSG_PLAY && msg.obj is Playback<*>) {
      (msg.obj as Playback<*>).play()
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

  internal fun play(playback: Playback<*>) {
    handler?.let {
      val delay = playback.delay.invoke()
      it.removeMessages(MSG_PLAY, playback)
      when {
        delay <= Playback.DELAY_INFINITE -> {
          // ignored
        }
        delay == 0L -> playback.play()
        else -> it.sendMessageDelayed(it.obtainMessage(MSG_PLAY, playback), delay)
      }
    }
  }

  internal fun pause(playback: Playback<*>) {
    handler?.removeMessages(MSG_PLAY, playback)
    playback.pause()
  }

  internal fun onPlaybackRemoved(playback: Playback<*>) {
    handler?.removeMessages(MSG_PLAY, playback)
  }
}