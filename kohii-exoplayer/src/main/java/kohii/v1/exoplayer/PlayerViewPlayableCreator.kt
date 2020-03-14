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
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.v1.BuildConfig
import kohii.v1.core.BridgeCreator
import kohii.v1.core.Common
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playable.Config
import kohii.v1.core.PlayableCreator
import kohii.v1.media.Media
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE

typealias PlayerViewBridgeCreatorFactory = Context.() -> BridgeCreator<PlayerView>

class PlayerViewPlayableCreator internal constructor(
  private val master: Master,
  private val bridgeCreatorFactory: PlayerViewBridgeCreatorFactory = defaultBridgeCreatorFactory
) : PlayableCreator<PlayerView>(PlayerView::class.java) {

  constructor(context: Context) : this(Master[context.applicationContext])

  companion object {
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 24 * 1024 * 1024L // 24 Megabytes

    // Only pass Application to this method.
    private val defaultBridgeCreatorFactory: PlayerViewBridgeCreatorFactory = {
      val userAgent = Common.getUserAgent(this, BuildConfig.LIB_NAME)
      val httpDataSource = DefaultHttpDataSourceFactory(userAgent)

      // ExoPlayerProvider
      val drmSessionManagerProvider = DefaultDrmSessionManagerProvider(this, httpDataSource)
      val playerProvider: ExoPlayerProvider = DefaultExoPlayerProvider(
          this,
          DefaultBandwidthMeterFactory(),
          drmSessionManagerProvider
      )

      // MediaSourceFactoryProvider
      val fileDir = getExternalFilesDir(null) ?: filesDir
      val contentDir = File(
          fileDir,
          CACHE_CONTENT_DIRECTORY
      )
      val mediaCache: Cache = SimpleCache(
          contentDir,
          LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
          ExoDatabaseProvider(this)
      )
      val upstreamFactory = DefaultDataSourceFactory(this, httpDataSource)
      val mediaSourceFactoryProvider: MediaSourceFactoryProvider =
        DefaultMediaSourceFactoryProvider(upstreamFactory, mediaCache)
      PlayerViewBridgeCreator(playerProvider, mediaSourceFactoryProvider)
    }
  }

  private val bridgeCreator: BridgeCreator<PlayerView> by lazy(NONE) {
    bridgeCreatorFactory(
        master.app
    )
  }

  override fun createPlayable(
    config: Config,
    media: Media
  ): Playable {
    return PlayerViewPlayable(
        master,
        media,
        config,
        bridgeCreator.createBridge(master.app, media)
    )
  }

  override fun cleanUp() {
    bridgeCreator.cleanUp()
  }

  class Builder(context: Context) {

    private val app = context.applicationContext

    private var bridgeCreatorFactory: PlayerViewBridgeCreatorFactory =
      defaultBridgeCreatorFactory

    fun setBridgeCreatorFactory(factory: PlayerViewBridgeCreatorFactory): Builder = apply {
      this.bridgeCreatorFactory = factory
    }

    fun build(): PlayableCreator<PlayerView> = PlayerViewPlayableCreator(
        Master[app],
        bridgeCreatorFactory
    )
  }
}
