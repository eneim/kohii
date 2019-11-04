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
import kotlin.properties.Delegates.observable

open class LazyViewPlayback<CONTAINER : ViewGroup, RENDERER : Any>(
  kohii: Kohii,
  playable: Playable<RENDERER>,
  manager: PlaybackManager,
  private val target: Target<CONTAINER, RENDERER>,
  options: Config,
  private val rendererPool: RendererPool<RENDERER>
) : ViewPlayback<CONTAINER, RENDERER>(
    kohii, playable, manager, target.container, options
) {

  init {
    require(target !is IdenticalTarget<*>) { "IdenticalTarget is not allowed here." }
  }

  private var _renderer: RENDERER? by observable<RENDERER?>(
      initialValue = null,
      onChange = { _, prev, next ->
        if (next === prev) return@observable
        // 1. Release previous value to Pool
        if (prev != null) {
          if (target.detachRenderer(prev)) {
            rendererPool.releaseRenderer(target, prev, playable.media)
            this.playable.onPlaybackInActive(this@LazyViewPlayback, prev)
          }
        }
        // 2. If next value is not null, attach and notify its value
        if (next != null) {
          target.attachRenderer(next)
          this.playable.onPlaybackActive(this@LazyViewPlayback, next)
        }
      })

  override val renderer: RENDERER?
    get() = this._renderer

  override fun beforePlayInternal() {
    super.beforePlayInternal()
    if (_renderer == null) {
      _renderer = rendererPool.acquireRenderer(this, this.target, playable.media)
    }
  }

  override fun afterPauseInternal() {
    super.afterPauseInternal()
    _renderer = null
  }

  override fun onRemoved() {
    _renderer = null
    super.onRemoved()
  }
}
