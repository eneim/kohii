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

package kohii.v1.exoplayer.internal

import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.v1.BuildConfig
import kohii.v1.core.Common
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playable.Config
import kohii.v1.core.PlayableCreator
import kohii.v1.media.Media
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE

class PlayerViewPlayableCreator(
  internal val master: Master
) : PlayableCreator<PlayerView>(PlayerView::class.java) {

  companion object {
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 24 * 1024 * 1024L // 24 Megabytes
  }

  private val app = master.app

  private val defaultBridgeProvider by lazy(NONE) {
    val userAgent = Common.getUserAgent(this.app, BuildConfig.LIB_NAME)
    val httpDataSource = DefaultHttpDataSourceFactory(userAgent)

    // ExoPlayerProvider
    val drmSessionManagerProvider =
      DefaultDrmSessionManagerProvider(this.app, httpDataSource)
    val playerProvider = DefaultExoPlayerProvider(
        this.app,
        DefaultBandwidthMeterFactory(),
        drmSessionManagerProvider
    )

    // MediaSourceFactoryProvider
    val fileDir = this.app.getExternalFilesDir(null) ?: this.app.filesDir
    val contentDir = File(
        fileDir,
        CACHE_CONTENT_DIRECTORY
    )
    val mediaCache: Cache =
      SimpleCache(
          contentDir,
          LeastRecentlyUsedCacheEvictor(
              CACHE_SIZE
          ),
          ExoDatabaseProvider(this.app)
      )
    val upstreamFactory =
      DefaultDataSourceFactory(this.app, httpDataSource)
    val mediaSourceFactoryProvider =
      DefaultMediaSourceFactoryProvider(upstreamFactory, mediaCache)
    PlayerViewBridgeCreator(playerProvider, mediaSourceFactoryProvider)
  }

  override fun createPlayable(
    config: Config,
    media: Media
  ): Playable {
    return PlayerViewPlayable(
        master,
        media,
        config,
        defaultBridgeProvider.createBridge(app, media)
    )
  }

  override fun cleanUp() {
    defaultBridgeProvider.cleanUp()
  }
}
