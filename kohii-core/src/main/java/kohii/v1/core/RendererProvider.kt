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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v1.media.Media

/**
 * A pool to cache the renderer for the Playback.
 */
interface RendererProvider : DefaultLifecycleObserver {

  /**
   * Returns a renderer for the [playback] that can be used to render the content of [media], or
   * `null` if no renderer is available.
   */
  fun acquireRenderer(
    playback: Playback,
    media: Media
  ): Any? = null

  /**
   * Releases the [renderer] back to the pool. Returns `true` if either the renderer is null (so
   * nothing needed to be done), or the renderer is successfully released back to the pool.
   */
  fun releaseRenderer(
    playback: Playback,
    media: Media,
    renderer: Any?
  ): Boolean = renderer == null

  /**
   * Cleans up this pool.
   */
  fun clear() = Unit

  override fun onDestroy(owner: LifecycleOwner) {
    clear()
  }
}
