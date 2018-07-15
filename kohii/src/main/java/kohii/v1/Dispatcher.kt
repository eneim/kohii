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
      MSG_TARGET_UNAVAILABLE -> manager.onTargetUnAvailable(msg.obj)
      MSG_TARGET_AVAILABLE -> manager.onTargetAvailable(msg.obj)
    }
  }

  /// APIs

  fun dispatchRefreshAll() {
    removeMessages(MSG_REFRESH)
    sendEmptyMessageDelayed(MSG_REFRESH, MSG_DELAY)
  }

  fun dispatchTargetUnAvailable(playback: Playback<*>) {
    val target = playback.getTarget()
    removeMessages(MSG_TARGET_UNAVAILABLE, target)
    obtainMessage(MSG_TARGET_UNAVAILABLE, -1, -1, target).sendToTarget()
  }

  fun dispatchTargetAvailable(playback: Playback<*>) {
    val target = playback.getTarget()
    removeMessages(MSG_TARGET_AVAILABLE, target)
    obtainMessage(MSG_TARGET_AVAILABLE, -1, -1, target).sendToTarget()
  }

  companion object {
    private const val MSG_DELAY = (5 * 1000 / 60).toLong()  // 5 frames

    private const val MSG_REFRESH = 1
    private const val MSG_TARGET_UNAVAILABLE = 2
    private const val MSG_TARGET_AVAILABLE = 3
  }
}
