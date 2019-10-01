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

import androidx.core.util.Pools.SimplePool
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.Media
import kohii.onEachAcquired

// A Pool must be kept in Activity's lifecycle. Because it has access to View created inside that.
open class DefaultRendererPool<RENDERER : Any>(
  val kohii: Kohii,
  private val poolSize: Int = 2,
  private val creator: RendererCreator<RENDERER>
) : RendererPool<RENDERER>, LifecycleObserver {

  companion object {
    private fun poolKey(
      containerType: Int,
      mediaType: Int
    ) = "${BuildConfig.LIBRARY_PACKAGE_NAME}::pool::$containerType::$mediaType"
  }

  // key = "kohii.v1::pool::${containerType}::${mediaType}"
  protected val keyToPool = HashMap<String, SimplePool<RENDERER>>()

  override fun <CONTAINER : Any> acquireRenderer(
    playback: Playback<RENDERER>,
    target: Target<CONTAINER, RENDERER>,
    media: Media
  ): RENDERER {
    val container = target.container
    val containerType = creator.getContainerType(container)
    val mediaType = creator.getMediaType(media)
    val poolKey = poolKey(containerType, mediaType)
    val pool = keyToPool[poolKey]
    return pool?.acquire() ?: creator.createRenderer(playback, container, mediaType)
  }

  override fun <CONTAINER : Any> releaseRenderer(
    target: Target<CONTAINER, RENDERER>,
    renderer: RENDERER,
    media: Media
  ) {
    val container = target.container
    val containerType = creator.getContainerType(container)
    val mediaType = creator.getMediaType(media)
    val poolKey = poolKey(containerType, mediaType)
    val pool = keyToPool.getOrPut(poolKey) { SimplePool(poolSize) }
    pool.release(renderer)
  }

  override fun cleanUp() {
    keyToPool.onEach { it.value.onEachAcquired { } }
        .clear()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    owner.lifecycle.removeObserver(this)
    kohii.cleanUpPool(owner, this)
  }
}
