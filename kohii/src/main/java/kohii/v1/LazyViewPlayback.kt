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

import android.view.ViewGroup

open class LazyViewPlayback<OUTPUT : Any>(
  kohii: Kohii,
  playable: Playable<OUTPUT>,
  manager: PlaybackManager,
  private val boxedTarget: Target<ViewGroup, OUTPUT>,
  options: Config,
  private val outputHolderPool: OutputHolderPool<ViewGroup, OUTPUT>
) : ViewPlayback<ViewGroup, OUTPUT>(
    kohii, playable, manager, boxedTarget.requireContainer(), options
) {

  private var _outputHolder: OUTPUT? = null

  override val outputHolder: OUTPUT?
    get() = this._outputHolder

  override fun beforePlayInternal() {
    super.beforePlayInternal()
    if (_outputHolder == null) {
      _outputHolder = outputHolderPool.acquireOutputHolder(this.boxedTarget, playable.media)
      if (_outputHolder != null) {
        this.playable.onPlayerActive(this, _outputHolder!!)
      }
    }
  }

  override fun afterPauseInternal() {
    super.afterPauseInternal()
    _outputHolder?.also {
      outputHolderPool.releaseOutputHolder(this.boxedTarget, it, playable.media)
      this.playable.onPlayerInActive(this, _outputHolder)
      _outputHolder = null
    }
  }

  override fun onRemoved() {
    _outputHolder?.also {
      outputHolderPool.releaseOutputHolder(this.boxedTarget, it, playable.media)
      _outputHolder = null
    }
    super.onRemoved()
  }
}
