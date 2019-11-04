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

package kohii.dev

import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.core.util.Pools.SimplePool
import kohii.media.Media
import kohii.onEachAcquired
import kohii.v1.BuildConfig

abstract class RecyclerRendererProvider<RENDERER : Any>(
  private val poolSize: Int = 2
) : RendererProvider<RENDERER> {

  companion object {
    private fun poolKey(
      containerType: Int,
      mediaType: Int
    ) = "${BuildConfig.LIBRARY_PACKAGE_NAME}::pool::$containerType::$mediaType"
  }

  private val keyToPool = ArrayMap<String, SimplePool<RENDERER>>()

  open fun getContainerType(container: Any): Int = 0

  open fun getMediaType(media: Media): Int = 0

  abstract fun <CONTAINER : ViewGroup> createRenderer(
    playback: Playback<CONTAINER>,
    mediaType: Int
  ): RENDERER

  override fun <CONTAINER : ViewGroup> acquireRenderer(
    playback: Playback<CONTAINER>,
    media: Media
  ): RENDERER {
    val containerType = getContainerType(playback.container)
    val mediaType = getMediaType(media)
    val poolKey = poolKey(containerType, mediaType)
    val pool = keyToPool[poolKey]
    return pool?.acquire() ?: createRenderer(playback, mediaType)
  }

  override fun <CONTAINER : ViewGroup> releaseRenderer(
    playback: Playback<CONTAINER>,
    media: Media,
    renderer: RENDERER?
  ) {
    if (renderer == null || playback.container === renderer) return
    val containerType = getContainerType(playback.container)
    val mediaType = getMediaType(media)
    val poolKey = poolKey(containerType, mediaType)
    val pool = keyToPool.getOrPut(poolKey) { SimplePool(poolSize) }
    pool.release(renderer)
  }

  override fun clear() {
    keyToPool.forEach { it.value.onEachAcquired { } }
  }
}
