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
import kohii.media.Media
import kohii.v1.Container
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlaybackManager
import kohii.v1.PlayerPool
import kohii.v1.Target
import kohii.v1.ViewPlayback

internal class LazyViewPlayback<PLAYER>(
  kohii: Kohii,
  media: Media,
  playable: Playable<PLAYER>,
  manager: PlaybackManager,
  container: Container,
  private val boxedTarget: Target<ViewGroup, PLAYER>,
  options: Config,
  private val playerPool: PlayerPool<ViewGroup, PLAYER>
) : ViewPlayback<ViewGroup, PLAYER>(
    kohii, media, playable, manager, container, boxedTarget.requireContainer(), options
) {

  private var _playerView: PLAYER? = null

  override val playerView: PLAYER?
    get() = this._playerView

  override fun play() {
    if (_playerView == null) {
      _playerView = playerPool.acquirePlayer(this.boxedTarget, this.media)
      if (this.playerCallback != null && _playerView != null) {
        this.playerCallback!!.onPlayerActive(_playerView!!)
      }
    }
    super.play()
  }

  override fun pause() {
    _playerView?.let {
      playerPool.releasePlayer(this.boxedTarget, it, media)
      if (this.playerCallback != null) {
        this.playerCallback!!.onPlayerInActive(_playerView)
      }
      _playerView = null
    }
    super.pause()
  }

  override fun onRemoved() {
    _playerView?.let {
      playerPool.releasePlayer(this.boxedTarget, it, media)
      _playerView = null
    }
    super.onRemoved()
  }
}
