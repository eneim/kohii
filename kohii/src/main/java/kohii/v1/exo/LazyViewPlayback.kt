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

package kohii.v1.exo

import android.view.ViewGroup
import kohii.v1.Kohii
import kohii.v1.OutputHolderManager
import kohii.v1.Playable
import kohii.v1.PlaybackManager
import kohii.v1.Target
import kohii.v1.TargetHost
import kohii.v1.ViewPlayback

internal class LazyViewPlayback<PLAYER>(
  kohii: Kohii,
  playable: Playable<PLAYER>,
  manager: PlaybackManager,
  targetHost: TargetHost,
  private val boxedTarget: Target<ViewGroup, PLAYER>,
  options: Config,
  private val outputHolderManager: OutputHolderManager<ViewGroup, PLAYER>
) : ViewPlayback<ViewGroup, PLAYER>(
    kohii, playable, manager, targetHost, boxedTarget.requireContainer(), options
) {

  private var _playerView: PLAYER? = null

  override val playerView: PLAYER?
    get() = this._playerView

  override fun play() {
    if (_playerView == null) {
      _playerView = outputHolderManager.acquirePlayer(this.boxedTarget, playable.media)
      if (this.availabilityCallback != null && _playerView != null) {
        this.availabilityCallback!!.onPlayerActive(this, _playerView!!)
      }
    }
    super.play()
  }

  override fun pause() {
    _playerView?.let {
      outputHolderManager.releasePlayer(this.boxedTarget, it, playable.media)
      if (this.availabilityCallback != null) {
        this.availabilityCallback!!.onPlayerInActive(this, _playerView)
      }
      _playerView = null
    }
    super.pause()
  }

  override fun onRemoved() {
    _playerView?.let {
      outputHolderManager.releasePlayer(this.boxedTarget, it, playable.media)
      _playerView = null
    }
    super.onRemoved()
  }
}
