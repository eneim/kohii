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
    }
  }

  /// APIs

  fun dispatchRefreshAll() {
    removeMessages(MSG_REFRESH)
    sendEmptyMessageDelayed(MSG_REFRESH, MSG_DELAY.toLong())
  }

  companion object {
    private const val MSG_DELAY = 5 * 1000 / 60
    private const val MSG_REFRESH = 1
  }
}
