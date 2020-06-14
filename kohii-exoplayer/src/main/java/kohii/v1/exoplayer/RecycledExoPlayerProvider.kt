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

package kohii.v1.exoplayer

import android.content.Context
import androidx.core.util.Pools
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.AudioComponent
import com.google.android.exoplayer2.util.Util
import kohii.v1.media.Media
import kohii.v1.onEachAcquired
import kotlin.math.max

/**
 * Base implementation of the [ExoPlayerProvider] that uses a [Pools.SimplePool] to store the
 * [Player] instance for reuse.
 *
 * @see DefaultExoPlayerProvider
 */
abstract class RecycledExoPlayerProvider(context: Context) : ExoPlayerProvider {

  companion object {
    // Max number of Player instance are cached in the Pool
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    internal val MAX_POOL_SIZE = max(Util.SDK_INT / 6, Runtime.getRuntime().availableProcessors())
  }

  private val context = context.applicationContext

  // Cache...
  private val plainPlayerPool = Pools.SimplePool<Player>(MAX_POOL_SIZE)

  /**
   * Create a new [Player] instance, given a [Context] of the Application.
   */
  abstract fun createExoPlayer(context: Context): Player

  override fun acquirePlayer(media: Media): Player {
    val result = plainPlayerPool.acquire() ?: createExoPlayer(context)
    result.playWhenReady = false
    if (result is AudioComponent) {
      result.setAudioAttributes(result.audioAttributes, false)
    }
    return result
  }

  override fun releasePlayer(
    media: Media,
    player: Player
  ) {
    // player.stop(true) // client must stop/do proper cleanup by itself.
    if (!plainPlayerPool.release(player)) {
      // No more space in pool --> this Player has no where to go --> release it.
      player.release()
    }
  }

  override fun cleanUp() {
    plainPlayerPool.onEachAcquired { it.release() }
  }
}
