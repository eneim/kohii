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

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kohii.media.Media

/**
 * @author eneim (2018/10/27).
 */
class DefaultMediaSourceFactoryProvider private constructor(
  private val offlineSourceHelper: OfflineSourceHelper? = null,
  upstreamFactory: DataSource.Factory,
  mediaCache: Cache? = null
) : MediaSourceFactoryProvider {

  constructor(
    upstreamFactory: DataSource.Factory,
    mediaCache: Cache? = null
  ) : this(null, upstreamFactory, mediaCache)

  @Suppress("unused") //
  constructor(
    upstreamFactory: DataSource.Factory,
    offlineSourceHelper: OfflineSourceHelper
  ) : this(offlineSourceHelper, upstreamFactory, offlineSourceHelper.downloadCache)

  private val dataSourceFactory: DataSource.Factory =
    if (mediaCache != null) {
      CacheDataSourceFactory(
          mediaCache,
          upstreamFactory,
          FileDataSourceFactory(), null,
          CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
      )
    } else {
      upstreamFactory
    }

  override fun provideMediaSourceFactory(media: Media): AdsMediaSource.MediaSourceFactory {
    when (@C.ContentType val type = Util.inferContentType(media.uri, media.type)) {
      C.TYPE_DASH ->
        return DashMediaSource.Factory(dataSourceFactory)
            .setManifestParser(
                FilteringManifestParser<DashManifest>(
                    DashManifestParser(), getOfflineStreamKeys(media.uri)
                )
            )
      C.TYPE_SS ->
        return SsMediaSource.Factory(dataSourceFactory)
            .setManifestParser(
                FilteringManifestParser<SsManifest>(
                    SsManifestParser(), getOfflineStreamKeys(media.uri)
                ) //
            )
      C.TYPE_HLS ->
        return HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(
                DefaultHlsPlaylistParserFactory(getOfflineStreamKeys(media.uri))
            )
      C.TYPE_OTHER ->
        return ExtractorMediaSource.Factory(dataSourceFactory)
      else -> {
        throw IllegalStateException("Unsupported type: $type")
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun getOfflineStreamKeys(uri: Uri): List<StreamKey> {
    return offlineSourceHelper?.offlineStreamKeys(uri) ?: emptyList()
  }
}
