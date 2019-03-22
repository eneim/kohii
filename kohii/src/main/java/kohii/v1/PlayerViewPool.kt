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

class PlayerViewPool<CONTAINER, PLAYER>(
  val size: Int,
  val adapter: PlayerAdapter<CONTAINER, PLAYER>
) : Cleanable {

  private val pools by lazy { SparseArrayCompat<SimplePool<PLAYER>>() }

  fun acquirePlayerView(
    container: CONTAINER,
    media: Media
  ): PLAYER {
    val type = adapter.getPlayerType(media)
    val pool = pools.getOrPut(type) { SimplePool(size) }
    val result = pool.acquire() ?: adapter.createPlayer(container, type)
    // adding result to container may throws exception if the result is added to other
    // container before. client must make sure it remove the result from old container first.
    adapter.attachPlayer(container, result)
    return result
  }

  fun releasePlayerView(
    container: CONTAINER,
    playerView: PLAYER,
    media: Media
  ) {
    if (adapter.detachPlayer(container, playerView)) {
      val type = adapter.getPlayerType(media)
      val pool = pools.getOrPut(type) { SimplePool(size) }
      pool.release(playerView)
    }
  }

  override fun cleanUp() {
    pools.forEach { pool, _ -> pool.onEachAcquired { } }
  }
}
