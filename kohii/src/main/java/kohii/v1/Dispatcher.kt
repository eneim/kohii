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
import android.os.Message

/**
 * @author eneim (2018/06/23).
 */
internal class Dispatcher(private val manager: Manager) : Handler() {

  override fun handleMessage(msg: Message) {
    super.handleMessage(msg)
    val what = msg.what
    when (what) {
      MSG_REFRESH -> manager.performRefreshAll()
      MSG_TARGET_UNAVAILABLE -> {
        (msg.obj as Playback<*>).getTarget()?.run { manager.onTargetUnAvailable(this) }
      }
      MSG_TARGET_AVAILABLE -> {
        (msg.obj as Playback<*>).getTarget()?.run { manager.onTargetAvailable(this) }
      }
    }
  }

  /// APIs

  fun dispatchRefreshAll() {
    // As late as possible.
    removeMessages(MSG_REFRESH)
    sendEmptyMessageDelayed(MSG_REFRESH, MSG_DELAY)
  }

  fun dispatchTargetUnAvailable(playback: Playback<*>) {
    // As early as possible
    if (!hasMessages(MSG_TARGET_UNAVAILABLE, playback)) {
      obtainMessage(MSG_TARGET_UNAVAILABLE, playback).sendToTarget()
    }
  }

  fun dispatchTargetAvailable(playback: Playback<*>) {
    // As early as possible
    if (!hasMessages(MSG_TARGET_AVAILABLE, playback)) {
      obtainMessage(MSG_TARGET_AVAILABLE, playback).sendToTarget()
    }
  }

  companion object {
    private const val MSG_DELAY = (5 * 1000 / 60).toLong()  // 5 frames

    private const val MSG_REFRESH = 1
    private const val MSG_TARGET_UNAVAILABLE = 2
    private const val MSG_TARGET_AVAILABLE = 3
  }
}
