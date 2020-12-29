/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.core

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import kohii.v1.debugOnly
import kohii.v1.logInfo

internal class MasterDispatcher(val master: Master) : Handler(Looper.getMainLooper()) {

  override fun handleMessage(msg: Message) {
    when (msg.what) {
      Master.MSG_CLEANUP -> master.cleanupPendingPlayables()
      Master.MSG_BIND_PLAYABLE -> {
        val container = msg.obj as ViewGroup
        debugOnly {
          val request = master.requests[container]
          if (request != null) {
            "Request bind: ${request.tag}, $container, ${request.playable}".logInfo()
          }
        }
        container.doOnAttach {
          master.requests.remove(it)
              ?.onBind()
        }
      }
      Master.MSG_RELEASE_PLAYABLE -> {
        val playable = (msg.obj as Playable)
        playable.onRelease()
      }
      Master.MSG_DESTROY_PLAYABLE -> {
        val playable = msg.obj as Playable
        val clearState = msg.arg1 == 0
        master.onTearDown(playable, clearState)
      }
    }
  }
}
