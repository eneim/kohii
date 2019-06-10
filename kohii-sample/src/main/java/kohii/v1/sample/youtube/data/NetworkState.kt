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

package kohii.v1.sample.youtube.data

import kohii.v1.sample.youtube.data.Status.FAILED
import kohii.v1.sample.youtube.data.Status.RUNNING
import kohii.v1.sample.youtube.data.Status.SUCCESS

enum class Status {
  RUNNING,
  SUCCESS,
  FAILED
}

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
  val status: Status,
  val msg: String? = null
) {
  companion object {
    val LOADED =
      NetworkState(SUCCESS)
    val LOADING =
      NetworkState(RUNNING)

    fun error(msg: String?) =
      NetworkState(FAILED, msg)
  }
}
