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

package kohii.v1

import android.os.Handler
import android.os.Message
import java.lang.ref.WeakReference

internal class ActivityContainerDispatcher(activityContainer: ActivityContainer) {

  private val handler = RootHandler(activityContainer)

  companion object {
    const val DELAY = 2 * 1000L / 60 /* about 2 frames */
    const val MSG_REFRESH = 1
  }

  internal fun dispatchRefresh() {
    handler.removeMessages(MSG_REFRESH)
    handler.sendEmptyMessageDelayed(MSG_REFRESH, DELAY)
  }

  internal fun onContainerDestroyed() {
    handler.removeCallbacksAndMessages(null)
  }

  class RootHandler(activityContainer: ActivityContainer) : Handler() {

    private val weakActivityContainer = WeakReference(activityContainer)

    override fun handleMessage(msg: Message?) {
      val activityContainer = weakActivityContainer.get()
      if (activityContainer != null) {
        val what = msg?.what
        when (what) {
          MSG_REFRESH -> {
            activityContainer.refreshPlaybacks()
          }
        }
      }
    }
  }
}
