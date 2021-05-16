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
import kohii.v1.logInfo

abstract class AbstractBridge<RENDERER : Any> : Bridge<RENDERER> {

  protected val eventListeners = PlayerEventListeners()
  protected val errorListeners = ErrorListeners()
  protected val volumeListeners = VolumeChangedListeners()

  override fun addEventListener(listener: PlayerEventListener) {
    this.eventListeners.add(listener)
  }

  override fun removeEventListener(listener: PlayerEventListener?) {
    this.eventListeners.remove(listener)
  }

  override fun addVolumeChangeListener(listener: VolumeChangedListener) {
    this.volumeListeners.add(listener)
  }

  override fun removeVolumeChangeListener(listener: VolumeChangedListener?) {
    this.volumeListeners.remove(listener)
  }

  override fun addErrorListener(errorListener: ErrorListener) {
    this.errorListeners.add(errorListener)
  }

  override fun removeErrorListener(errorListener: ErrorListener?) {
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

  override var videoSize: VideoSize = VideoSize.ORIGINAL

  // For backward compatibility.
  override var playerParameters: PlayerParameters = PlayerParameters.DEFAULT
}
