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

package kohii.v1.x

import android.content.Context
import androidx.core.util.Pools
import androidx.media2.player.MediaPlayer
import com.google.android.exoplayer2.util.Util
import kohii.v1.media.Media
import kohii.v1.onEachAcquired
import kotlin.math.max

internal class DefaultMediaPlayerProvider(
  private val context: Context
) : MediaPlayerProvider {

  companion object {
    // Max number of Player instance are cached in the Pool
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    internal val MAX_POOL_SIZE = max(Util.SDK_INT / 6, Runtime.getRuntime().availableProcessors())
  }

  private val plainPlayerPool = Pools.SimplePool<MediaPlayer>(
      MAX_POOL_SIZE
  )

  override fun acquirePlayer(media: Media): MediaPlayer {
    return if (media.mediaDrm != null) MediaPlayer(context.applicationContext) else run {
      return@run plainPlayerPool.acquire() ?: MediaPlayer(context.applicationContext)
    }
  }

  override fun releasePlayer(
    media: Media,
    player: MediaPlayer
  ) {
    if (media.mediaDrm == null) plainPlayerPool.release(player)
  }

  override fun cleanUp() {
    plainPlayerPool.onEachAcquired { it.close() }
  }
}
