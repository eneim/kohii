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
import android.support.annotation.RestrictTo
import android.support.v4.util.Pools
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Util.getUserAgent
import kohii.media.VolumeInfo
import kohii.v1.BuildConfig.LIB_NAME
import kohii.v1.Playable
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

  val context: Context = context.applicationContext  // Application context
  val appName: String = getUserAgent(context, LIB_NAME)
  private val playerFactories = HashMap<Config, PlayerFactory>()
  private val sourceFactories = HashMap<Config, MediaSourceFactory>()
  private val drmSessionManagerFactories = HashMap<Config, DrmSessionManagerFactory>()
  private val mapConfigToPool = HashMap<Config, Pools.Pool<Player>>()

  init {
    // Adapt from ExoPlayer demo app. Start this on demand.
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    if (CookieHandler.getDefault() !== cookieManager) {
      CookieHandler.setDefault(cookieManager)
    }
  }

  private fun getPool(config: Config): Pools.Pool<Player> {
    var pool: Pools.Pool<Player>? = mapConfigToPool[config]
    if (pool == null) {
      pool = Pools.SimplePool(MAX_POOL_SIZE)
      mapConfigToPool[config] = pool
    }

    return pool
  }

  fun acquirePlayer(config: Config): Player {
    var player = getPool(config).acquire()
    if (player == null) player = (playerFactories[config] ?: DefaultPlayerFactory(this,
        config).also {
      playerFactories[config] = it
    }).createPlayer()
    return player
  }

  fun releasePlayer(player: Player, config: Config) {
    getPool(config).release(player)
  }

  fun createMediaSource(builder: Playable.Builder): MediaSource {
    return (sourceFactories[builder.config] ?: DefaultMediaSourceFactory(this,
        builder.config).also {
      this.addMediaSourceFactory(builder.config, it)
    }).createMediaSource(builder)
  }

  /// Public API

  @Suppress("unused")
  fun addPlayerFactory(config: Config, playerFactory: PlayerFactory) {
    playerFactories[config] = playerFactory
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun addMediaSourceFactory(config: Config, mediaSourceFactory: MediaSourceFactory) {
    sourceFactories[config] = mediaSourceFactory
  }

  companion object {
    // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
    internal val MAX_POOL_SIZE = Math.max(Util.SDK_INT / 6, getRuntime().availableProcessors())

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
