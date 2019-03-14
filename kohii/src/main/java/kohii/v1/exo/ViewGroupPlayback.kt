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
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Container
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlaybackManager
import kohii.v1.ViewPlayback
import kohii.v1.ViewPool
import kohii.v1.exo.ViewGroupPlayable.PlayerViewProvider

// V: actual View to play on. Be a PlayerView or SurfaceView or something valid.
class ViewGroupPlayback(
  kohii: Kohii,
  playable: Playable<ViewGroup>,
  manager: PlaybackManager,
  container: Container,
  target: ViewGroup,
  options: Config,
  private val viewPool: ViewPool<PlayerView>
) : ViewPlayback<ViewGroup>(kohii, playable, manager, container, target, options),
    PlayerViewProvider {

  private var _playerView: PlayerView? = null

  override val playerView: PlayerView?
    get() = this._playerView

  override fun play() {
    if (_playerView == null) _playerView = viewPool.acquireForContainer(this.target)
    super.play()
  }

  override fun pause() {
    _playerView?.let {
      viewPool.releaseFromContainer(this.target, it)
      _playerView = null
    }
    super.pause()
  }

  override fun onRemoved() {
    _playerView?.let {
      viewPool.releaseFromContainer(this.target, it)
      _playerView = null
    }
    super.onRemoved()
  }
}