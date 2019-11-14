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

package kohii.core

import android.app.Application
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.ExoPlayer
import kohii.core.Playable.Config
import kohii.media.Media
import kohii.v1.BuildConfig
import kohii.v1.Kohii
import kohii.v1.exo.DefaultBandwidthMeterFactory
import kohii.v1.exo.DefaultDrmSessionManagerProvider
import kohii.v1.exo.DefaultExoPlayerProvider
import kohii.v1.exo.DefaultMediaSourceFactoryProvider
import kohii.v1.exo.PlayerViewBridgeProvider
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE

internal class PlayerViewPlayableCreator(
  val app: Application
) : Creator<PlayerView>(PlayerView::class.java) {

  companion object {
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 24 * 1024 * 1024L // 24 Megabytes
  }

  @ExoPlayer
  internal val defaultBridgeProvider by lazy(NONE) {
    val userAgent = Kohii.getUserAgent(this.app, BuildConfig.LIB_NAME)
    val httpDataSource =
      DefaultHttpDataSourceFactory(userAgent)

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
    val contentDir = File(fileDir, CACHE_CONTENT_DIRECTORY)
    val mediaCache: Cache =
      SimpleCache(
          contentDir,
          LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
          ExoDatabaseProvider(this.app)
      )
    val upstreamFactory =
      DefaultDataSourceFactory(this.app, httpDataSource)
    val mediaSourceFactoryProvider =
      DefaultMediaSourceFactoryProvider(upstreamFactory, mediaCache)
    PlayerViewBridgeProvider(playerProvider, mediaSourceFactoryProvider)
  }

  override fun createPlayable(
    master: Master,
    config: Config,
    media: Media
  ): Playable<PlayerView> {
    return PlayerViewPlayable(
        master,
        media,
        config,
        defaultBridgeProvider.provideBridge(master.kohii, media)
    )
  }

  override fun createRebinder(tag: Any): Rebinder<PlayerView> {
    return PlayerViewRebinder(tag)
  }
}
