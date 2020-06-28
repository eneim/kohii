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

package kohii.v1.utils

import kohii.v1.core.Engine

/**
 * Singleton Holder
 */
open class Capsule<T : Any, in A>(
  creator: (A) -> T,
  onCreate: ((T) -> Unit) = { if (it is Engine<*>) it.master.registerEngine(it) }
) {
  @Volatile private var instance: T? = null

  private var creator: ((A) -> T)? = creator
  private var onCreate: ((T) -> Unit)? = onCreate

  protected fun getInstance(arg: A): T {
    val check = instance
    if (check != null) {
      return check
    }

    return synchronized(this) {
      val doubleCheck = instance
      if (doubleCheck != null) {
        doubleCheck
      } else {
        val created = requireNotNull(creator)(arg)
        requireNotNull(onCreate)(created)
        instance = created
        creator = null
        onCreate = null
        created
      }
    }
  }

  fun get(arg: A): T = getInstance(arg)
}
