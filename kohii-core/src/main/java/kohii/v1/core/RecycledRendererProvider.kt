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

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.core.util.Pools.SimplePool
import kohii.v1.media.Media
import kohii.v1.onEachAcquired

abstract class RecycledRendererProvider @JvmOverloads constructor(
  private val poolSize: Int = 2
) : RendererProvider {

  private val pools = SparseArrayCompat<SimplePool<Any>>(2)

  @CallSuper
  override fun acquireRenderer(
    playback: Playback,
    media: Media
  ): Any {
    val rendererType = getRendererType(playback.container, media)
    val pool = pools.get(rendererType)
    return pool?.acquire() ?: createRenderer(playback, rendererType)
  }

  // Test: releaseRenderer(any(), any(), null) must return true.
  @CallSuper
  override fun releaseRenderer(
    playback: Playback,
    media: Media,
    renderer: Any?
  ): Boolean {
    if (renderer == null) return true
    val rendererType = getRendererType(playback.container, media)
    val pool = pools.get(rendererType) ?: SimplePool<Any>(poolSize).also {
      pools.put(rendererType, it)
    }
    return pool.release(renderer)
  }

  @CallSuper
  override fun clear() {
    pools.forEach { _, value ->
      value.onEachAcquired(::onClear)
    }
  }

  protected open fun getRendererType(
    container: ViewGroup,
    media: Media
  ): Int = 0

  // Must always create new Renderer.
  protected abstract fun createRenderer(
    playback: Playback,
    rendererType: Int
  ): Any

  protected open fun onClear(renderer: Any) = Unit
}
