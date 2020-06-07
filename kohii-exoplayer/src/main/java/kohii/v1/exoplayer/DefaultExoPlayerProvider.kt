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
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.Util
import kohii.v1.media.Media
import kohii.v1.onEachAcquired
import kotlin.math.max

/**
 * @author eneim (2018/10/27).
 */
class DefaultExoPlayerProvider @JvmOverloads constructor(
  private val context: Context,
  private val bandwidthMeterFactory: BandwidthMeterFactory = DefaultBandwidthMeterFactory(),
  private val loadControl: LoadControl = DefaultLoadControl(),
  private val renderersFactory: RenderersFactory = DefaultRenderersFactory(
      context.applicationContext
  ).setExtensionRendererMode(EXTENSION_RENDERER_MODE_OFF)
) : ExoPlayerProvider {

  companion object {
    // Max number of Player instance are cached in the Pool
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    internal val MAX_POOL_SIZE = max(Util.SDK_INT / 6, Runtime.getRuntime().availableProcessors())
  }

  // Cache...
  private val plainPlayerPool = Pools.SimplePool<Player>(MAX_POOL_SIZE)

  override fun acquirePlayer(media: Media): Player {
    val result = plainPlayerPool.acquire() ?: KohiiExoPlayer(
        context,
        renderersFactory,
        DefaultTrackSelector(context.applicationContext),
        loadControl,
        bandwidthMeterFactory.createBandwidthMeter(this.context),
        Util.getLooper()
    )

    result.playWhenReady = false
    (result as? SimpleExoPlayer)?.also { it.setAudioAttributes(it.audioAttributes, false) }
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
