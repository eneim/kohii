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

package kohii.v1.core

import androidx.annotation.CallSuper
import com.google.android.exoplayer2.Player
import kohii.v1.logInfo

abstract class AbstractBridge<RENDERER : Any> : Bridge<RENDERER> {

  protected val eventListeners = PlayerEventListeners()
  protected val errorListeners = ErrorListeners()
  protected val volumeListeners = VolumeChangedListeners()

  final override fun addEventListener(listener: Player.Listener) {
    this.eventListeners.add(listener)
  }

  final override fun removeEventListener(listener: Player.Listener?) {
    this.eventListeners.remove(listener)
  }

  final override fun addVolumeChangeListener(listener: VolumeChangedListener) {
    this.volumeListeners.add(listener)
  }

  final override fun removeVolumeChangeListener(listener: VolumeChangedListener?) {
    this.volumeListeners.remove(listener)
  }

  final override fun addErrorListener(errorListener: ErrorListener) {
    this.errorListeners.add(errorListener)
  }

  final override fun removeErrorListener(errorListener: ErrorListener?) {
    this.errorListeners.remove(errorListener)
  }

  @CallSuper
  override fun play() {
    "Bridge#play $this".logInfo()
  }

  @CallSuper
  override fun pause() {
    "Bridge#pause $this".logInfo()
  }

  // For backward compatibility.
  override var playerParameters: PlayerParameters = PlayerParameters.DEFAULT
}
