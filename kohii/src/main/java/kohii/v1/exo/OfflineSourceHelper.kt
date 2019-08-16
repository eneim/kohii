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

package kohii.v1.exo

import android.net.Uri
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.Cache

// [20190802] Will provide an implementation after 1.0.0 final.
interface OfflineSourceHelper {

  val downloadCache: Cache

  val dataSourceFactory: DataSource.Factory

  fun getDownloadRequest(uri: Uri): DownloadRequest

  fun getMediaSourceFactory(uri: Uri): AdsMediaSource.MediaSourceFactory {
    val downloadRequest = this.getDownloadRequest(uri)
    if (downloadRequest.uri != uri) throw IllegalArgumentException(
        "Download request is for different Uri. Expected $uri, found ${downloadRequest.uri}"
    )

    return when (downloadRequest.type) {
      DownloadRequest.TYPE_DASH ->
        DashMediaSource.Factory(dataSourceFactory).setStreamKeys(downloadRequest.streamKeys)
      DownloadRequest.TYPE_SS ->
        SsMediaSource.Factory(dataSourceFactory).setStreamKeys(downloadRequest.streamKeys)
      DownloadRequest.TYPE_HLS ->
        HlsMediaSource.Factory(dataSourceFactory).setStreamKeys(downloadRequest.streamKeys)
      DownloadRequest.TYPE_PROGRESSIVE ->
        ProgressiveMediaSource.Factory(dataSourceFactory)
      else -> {
        throw IllegalStateException("Unsupported type: ${downloadRequest.type}")
      }
    }
  }

  // if empty uris --> purge all downloaded content.
  fun purge(vararg uris: Uri)
}
