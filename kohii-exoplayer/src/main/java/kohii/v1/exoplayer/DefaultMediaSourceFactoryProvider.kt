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
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kohii.v1.BuildConfig
import kohii.v1.core.Common
import kohii.v1.exoplayer.ExoPlayerCache.lruCacheSingleton
import kohii.v1.media.Media

/**
 * @author eneim (2018/10/27).
 */
class DefaultMediaSourceFactoryProvider @JvmOverloads constructor(
  dataSourceFactory: DataSource.Factory,
  private val drmSessionManagerProvider: DrmSessionManagerProvider? = null,
  mediaCache: Cache? = null
) : MediaSourceFactoryProvider {

  constructor(context: Context, dataSourceFactory: HttpDataSource.Factory) : this(
      dataSourceFactory = DefaultDataSourceFactory(context, dataSourceFactory),
      drmSessionManagerProvider = DefaultDrmSessionManagerProvider(context, dataSourceFactory),
      mediaCache = lruCacheSingleton.get(context)
  )

  constructor(context: Context) : this(
      context,
      DefaultHttpDataSourceFactory(Common.getUserAgent(context, BuildConfig.LIB_NAME))
  )

  private val dataSourceFactory: DataSource.Factory = if (mediaCache != null) {
    CacheDataSourceFactory(
        mediaCache,
        dataSourceFactory,
        CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
    )
  } else {
    dataSourceFactory
  }

  override fun provideMediaSourceFactory(media: Media): MediaSourceFactory {
    @C.ContentType val type = Util.inferContentType(media.uri, media.type)

    if (media is HybridMediaItem) {
      return object : MediaSourceFactory {
        override fun getSupportedTypes(): IntArray {
          return intArrayOf(type)
        }

        override fun createMediaSource(uri: Uri?): MediaSource {
          return media.mediaSource
        }

        override fun setDrmSessionManager(drmSessionManager: DrmSessionManager<*>?): MediaSourceFactory {
          return this
        }
      }
    }

    val factory = when (type) {
      C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
      C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
      C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
      C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
      else -> {
        throw IllegalStateException("Unsupported type: $type")
      }
    }
    val drmSessionManager = drmSessionManagerProvider?.provideDrmSessionManager(media)
    if (drmSessionManager != null) factory.setDrmSessionManager(drmSessionManager)
    return factory
  }
}
