/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

import androidx.annotation.MainThread

object CompatHelper {

  private val defaultConfig: CompatConfig = CompatConfig()
  private var customConfig: CompatConfig? = null

  val compatConfig: CompatConfig get() = customConfig ?: defaultConfig

  @MainThread
  fun setCompatConfig(compatConfig: CompatConfig) {
    check(customConfig == null || customConfig == compatConfig) {
      "Another CompatConfig is already set."
    }
    customConfig = compatConfig
  }
}

data class CompatConfig(
  val useLegacyPlaybackInfoStore: Boolean = false
)
