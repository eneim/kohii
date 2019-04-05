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

import androidx.collection.SparseArrayCompat
import androidx.core.util.Pools.SimplePool
import kohii.forEach
import kohii.getOrPut
import kohii.media.Media
import kohii.onEachAcquired

open class OutputHolderPool<CONTAINER, OUTPUT>(
  private val poolSize: Int,
  private val creator: OutputHolderCreator<CONTAINER, OUTPUT>
) : Cleanable {

  protected val pools = SparseArrayCompat<SimplePool<OUTPUT>>()

  fun acquireOutputHolder(
    target: Target<CONTAINER, OUTPUT>,
    media: Media
  ): OUTPUT {
    val type = creator.getMediaType(media)
    val pool = pools.getOrPut(type) { SimplePool(poolSize) }
    val result = pool.acquire() ?: creator.createOutputHolder(target.requireContainer(), type)
    // adding result to container may throws exception if the result is added to other
    // container before. client must make sure it remove the result from old container first.
    target.attachOutputHolder(result)
    return result
  }

  fun releaseOutputHolder(
    target: Target<CONTAINER, OUTPUT>,
    player: OUTPUT,
    media: Media
  ) {
    if (target.detachOutputHolder(player)) {
      val type = creator.getMediaType(media)
      val pool = pools.getOrPut(type) { SimplePool(poolSize) }
      pool.release(player)
    }
  }

  override fun cleanUp() {
    pools.forEach { pool, _ -> pool.onEachAcquired { /* no-op */ } }
  }
}
