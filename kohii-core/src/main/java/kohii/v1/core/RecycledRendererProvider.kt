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
import androidx.collection.ArrayMap
import androidx.core.util.Pools.SimplePool
import kohii.v1.media.Media
import kohii.v1.onEachAcquired
import kohii.v1.BuildConfig

abstract class RecycledRendererProvider(private val poolSize: Int) : RendererProvider {

  constructor() : this(2)

  companion object {
    private fun poolKey(
      containerType: Int,
      mediaType: Int
    ) = "${BuildConfig.LIBRARY_PACKAGE_NAME}::pool::$containerType::$mediaType"
  }

  private val keyToPool = ArrayMap<String, SimplePool<Any>>()

  open fun getContainerType(container: Any): Int = 0

  open fun getMediaType(media: Media): Int = 0

  // Must always create new Renderer.
  abstract fun createRenderer(
    playback: Playback,
    mediaType: Int
  ): Any

  @CallSuper
  override fun acquireRenderer(
    playback: Playback,
    media: Media
  ): Any {
    // The Container is also a Renderer, we return it right away.
    val playable = requireNotNull(playback.playable)
    if (playable.config.rendererType.isAssignableFrom(playback.container.javaClass)) {
      return playback.container
    }
    val containerType = getContainerType(playback.container)
    val mediaType = getMediaType(media)
    val poolKey = poolKey(
        containerType, mediaType
    )
    val pool = keyToPool[poolKey]
    return pool?.acquire() ?: createRenderer(playback, mediaType)
  }

  @CallSuper
  override fun releaseRenderer(
    playback: Playback,
    media: Media,
    renderer: Any?
  ) {
    if (renderer == null || playback.container === renderer) return
    val containerType = getContainerType(playback.container)
    val mediaType = getMediaType(media)
    val poolKey = poolKey(
        containerType, mediaType
    )
    val pool = keyToPool.getOrPut(poolKey) { SimplePool(poolSize) }
    pool.release(renderer)
  }

  @CallSuper
  override fun clear() {
    keyToPool.forEach { it.value.onEachAcquired { renderer -> onClear(renderer) } }
  }

  protected open fun onClear(renderer: Any) {}
}
