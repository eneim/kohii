/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.exo

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.core.util.Pools
import androidx.core.util.Pools.Pool
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Util.getUserAgent
import kohii.media.VolumeInfo
import kohii.v1.BuildConfig.LIB_NAME
import kohii.v1.Playable
import java.io.File
import java.lang.Runtime.getRuntime
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.HashMap

/**
 * Store for [ExoPlayer]s
 *
 * @author eneim (2018/06/24).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) //
class ExoStore internal constructor(context: Context) {

  private val defaultCache = SimpleCache(File(context.cacheDir, "kohii").also { it.mkdir() },
      LeastRecentlyUsedCacheEvictor(MAX_CACHE))
  private val mapConfigToPool = HashMap<Config, Pools.Pool<Player>>()
  private val drmSessionManagerFactories = HashMap<Config, DrmSessionManagerFactory>()

  val context: Context = context.applicationContext  // Application context
  val appName: String = getUserAgent(context, LIB_NAME)
  val defaultConfig = Config.DEFAULT_CONFIG.copy(cache = defaultCache)
  val mapConfigToPlayerFactory = HashMap<Config, PlayerFactory>()
  val mapConfigToSourceFactory = HashMap<Config, MediaSourceFactory>()

  init {
    // Adapt from ExoPlayer demo app.
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    if (CookieHandler.getDefault() !== cookieManager) {
      CookieHandler.setDefault(cookieManager)
    }
  }

  /**
   * Get a Pool for Players following Flyweight pattern. For internal use only.
   *
   * @param config the [Config] using which we can construct a new [Player] ([KohiiPlayer])
   * @return a [Pools.Pool] for [Player]
   */
  private fun getPool(config: Config): Pools.Pool<Player> {
    return mapConfigToPool[config] ?: // find from cache or create new one.
    Pools.SimplePool<Player>(MAX_POOL_SIZE).also { mapConfigToPool[config] = it }
  }

  internal fun acquirePlayer(config: Config): Player {
    var player = getPool(config).acquire()
    if (player == null) { // cannot find one from pool, create new.
      player = (mapConfigToPlayerFactory[config] ?: DefaultPlayerFactory(this, config)
          .also { mapConfigToPlayerFactory[config] = it }
          ).createPlayer(config.mediaDrm)
    }

    (player as? SimpleExoPlayer)?.audioAttributes = AudioAttributes.Builder().setContentType(
        C.CONTENT_TYPE_MOVIE).build()

    return player
  }

  internal fun releasePlayer(player: Player, config: Config) {
    getPool(config).release(player) // release back to pool for reuse.
  }

  internal fun cleanUp() {
    val iterator = mapConfigToPool.entries.iterator()
    while (iterator.hasNext()) {
      val pool = iterator.next().value
      pool.onEachAcquired { it.release() }
      iterator.remove()
    }
  }

  internal fun createMediaSource(builder: Playable.Builder): MediaSource {
    return (mapConfigToSourceFactory[builder.config] ?: // find a factory or create new default one.
    DefaultMediaSourceFactory(this, builder.config) //
        .also { mapConfigToSourceFactory[builder.config] = it }
        )
        .createMediaSource(builder)
  }

  companion object {
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    internal val MAX_POOL_SIZE = Math.max(Util.SDK_INT / 6, getRuntime().availableProcessors())
    internal const val MAX_CACHE = 64 * 102 * 1024.toLong()

    @SuppressLint("StaticFieldLeak")
    private var exoStore: ExoStore? = null

    operator fun get(context: Context) = exoStore ?: synchronized(this) {
      exoStore ?: ExoStore(context).also { exoStore = it }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) //
    fun setVolumeInfo(player: Player, volume: VolumeInfo) {
      when (player) {
        is KohiiPlayer -> player.setVolumeInfo(volume)
        is SimpleExoPlayer -> {
          if (volume.mute) {
            player.volume = 0f
          } else {
            player.volume = volume.volume
          }
        }
        else -> throw RuntimeException(player.javaClass.simpleName + " doesn't support this.")
      }
    }

    @Suppress("unused")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) //
    fun getVolumeInfo(player: Player): VolumeInfo {
      return when (player) {
        is KohiiPlayer -> VolumeInfo(player.volumeInfo)
        is SimpleExoPlayer -> {
          val volume = player.volume
          VolumeInfo(volume == 0f, volume)
        }
        else -> throw RuntimeException(player.javaClass.simpleName + " doesn't support this.")
      }
    }
  }
}

fun <T> Pool<T>.onEachAcquired(action: (T) -> Unit) {
  var item: T?
  do {
    item = this.acquire()
    if (item == null) break
    else action(item)
  } while (true)
}