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

package kohii.core

import kotlin.properties.Delegates

// TODO complete this design to make it work with BroadcastReceiver.
// Expected usage: playback.observe(switch)
// Pass a Switch instance to a BroadcastReceiver, and use the same instance to observe from Playback.
class Switch(private val value: Boolean) {

  private var actual: Boolean by Delegates.observable(
      initialValue = this.value,
      onChange = { _, oldValue, newValue ->
        if (oldValue != newValue) callbacks.forEach { it.onSwitch(this, oldValue, newValue) }
      }
  )

  interface Callback {
    fun onSwitch(
      switch: Switch,
      from: Boolean,
      to: Boolean
    )
  }

  private val callbacks by lazy { mutableSetOf<Callback>() }

  internal fun addCallback(callback: Callback) {
    callbacks.add(callback)
  }

  internal fun removeCallback(callback: Callback?) {
    callbacks.remove(callback)
  }

  fun setValue(value: Boolean) {
    this.actual = value
  }

  fun getValue(): Boolean = this.actual
}
