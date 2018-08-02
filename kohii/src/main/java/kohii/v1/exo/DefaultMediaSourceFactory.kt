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

import android.text.TextUtils.isEmpty
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util.inferContentType
import kohii.v1.Playable

/**
 * @author eneim (2018/06/25).
 */
class DefaultMediaSourceFactory(
    store: ExoStore,
    config: Config = Config.DEFAULT_CONFIG
) : MediaSourceFactory {

  private val meter = config.meter
  private val cache = config.cache
  private val mediaDataSourceFactory: DataSource.Factory by lazy {
    var value = DefaultDataSourceFactory(store.context, store.appName, meter) as DataSource.Factory
    if (cache != null) value = CacheDataSourceFactory(cache, value)
    value
  }
  private val manifestDataSourceFactory = DefaultDataSourceFactory(store.context, store.appName)

  override fun createMediaSource(builder: Playable.Builder): MediaSource {
    @C.ContentType val type =
        if (isEmpty(builder.mediaType)) inferContentType(builder.contentUri)
        else inferContentType("." + builder.mediaType!!)
    return when (type) {
      C.TYPE_SS ->
        SsMediaSource.Factory(
            DefaultSsChunkSource.Factory(mediaDataSourceFactory),
            manifestDataSourceFactory
        ).createMediaSource(builder.contentUri)
      C.TYPE_DASH ->
        DashMediaSource.Factory(
            DefaultDashChunkSource.Factory(mediaDataSourceFactory),
            manifestDataSourceFactory
        ).createMediaSource(builder.contentUri)
      C.TYPE_HLS ->
        HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(builder.contentUri)
      C.TYPE_OTHER ->
        ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(builder.contentUri)
      else -> throw IllegalStateException("Unsupported type: $type")
    }
  }
}
