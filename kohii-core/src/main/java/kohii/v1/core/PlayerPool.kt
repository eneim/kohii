/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.util.Pools
import com.google.android.exoplayer2.util.Util
import kohii.v1.media.Media
import kohii.v1.onEachAcquired
import kotlin.math.max

/**
 * Definition of a pool to provide [PLAYER] instance for the consumer.
 */
abstract class PlayerPool<PLAYER> @JvmOverloads constructor(
  @IntRange(from = 1) poolSize: Int = DEFAULT_POOL_SIZE
) {

  companion object {
    // Max number of Player instance are cached in the Pool
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    val DEFAULT_POOL_SIZE =
      max(Util.SDK_INT / 6, max(Runtime.getRuntime().availableProcessors(), 1))
  }

  init {
    require(poolSize > 0) { "Pool size must be positive." }
  }

  private val playerPool = Pools.SimplePool<PLAYER>(poolSize)

  /**
   * Return `true` if a [PLAYER] instance can be reused to play the [media], `false` otherwise. If
   * this method returns `false`, this pool will always create new [PLAYER] instance and never put
   * that instance back to pool.
   *
   * @param media The [Media] object.
   */
  protected open fun recyclePlayerForMedia(media: Media): Boolean = true

  /**
   * Reset the internal state of the [player] instance before putting it back to the pool.
   *
   * @param player The [PLAYER] instance to reset.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  open fun resetPlayer(player: PLAYER) = Unit

  /**
   * Create a new [PLAYER] instance that can be used to play the [media] object.
   *
   * @param media The [Media] object.
   * @return a [PLAYER] instance that can be used to play the [media].
   */
  abstract fun createPlayer(media: Media): PLAYER

  /**
   * Destroy the [PLAYER] instance.
   *
   * @param player The [PLAYER] instance.
   */
  abstract fun destroyPlayer(player: PLAYER)

  /**
   * Acquire a [PLAYER] that can be used to play the [media] from the pool. If there is no available
   * instance in the pool, this method will create a new one.
   *
   * @param media The [Media] object.
   * @return a [PLAYER] instance that can be used to play the [media].
   */
  fun getPlayer(media: Media): PLAYER {
    if (!recyclePlayerForMedia(media)) return createPlayer(media)
    return playerPool.acquire() ?: createPlayer(media)
  }

  /**
   * Release an unused [PLAYER] to the pool. If the pool is already full, this method must destroy
   * the [PLAYER] instance. Return `true` if the instance is successfully put back to the pool, or
   * `false` otherwise.
   *
   * @param media The [Media] object.
   * @param player The [PLAYER] to be put back to the pool.
   * @return `true` if the instance is successfully put back to the pool, or `false` otherwise.
   */
  fun putPlayer(
    media: Media,
    player: PLAYER
  ): Boolean {
    return if (!recyclePlayerForMedia(media) || !playerPool.release(player)) {
      destroyPlayer(player)
      false
    } else {
      resetPlayer(player)
      true
    }
  }

  /**
   * Destroy all available [PLAYER] instances in the pool.
   */
  @CallSuper
  open fun clear() {
    playerPool.onEachAcquired {
      destroyPlayer(it)
    }
  }
}
