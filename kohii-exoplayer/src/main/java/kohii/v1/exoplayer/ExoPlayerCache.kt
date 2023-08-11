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
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.v1.exoplayer.ExoPlayerCache.downloadCacheSingleton
import kohii.v1.exoplayer.ExoPlayerCache.lruCacheSingleton
import kohii.v1.utils.Capsule
import java.io.File

/**
 * A convenient object to help creating and reusing a [Cache] for the media content. It supports
 * a [lruCacheSingleton] which is a [SimpleCache] that uses the [LeastRecentlyUsedCacheEvictor]
 * internally, and a [downloadCacheSingleton] which is a [SimpleCache] that doesn't evict cache,
 * which is useful to store downloaded content.
 */
object ExoPlayerCache {

  private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
  private const val DOWNLOAD_CONTENT_DIRECTORY = "kohii_content_download"
  private const val CACHE_SIZE = 24 * 1024 * 1024L // 24 Megabytes

  private val lruCacheCreator: (Context) -> Cache = { context ->
    SimpleCache(
      File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        CACHE_CONTENT_DIRECTORY
      ),
      LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
      StandaloneDatabaseProvider(context)
    )
  }

  private val downloadCacheCreator: (Context) -> Cache = { context ->
    SimpleCache(
      File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        DOWNLOAD_CONTENT_DIRECTORY
      ),
      NoOpCacheEvictor(),
      StandaloneDatabaseProvider(context)
    )
  }

  /**
   * A reusable [Cache] that uses the [LeastRecentlyUsedCacheEvictor] internally.
   *
   * Usage:
   *
   * ```kotlin
   * val cache = ExoPlayerCache.lruCacheSingleton.get(context)
   * ```
   */
  val lruCacheSingleton = Capsule(lruCacheCreator)

  /**
   * A reusable [Cache] that uses the [NoOpCacheEvictor] internally.
   *
   * Usage:
   *
   * ```kotlin
   * val cache = ExoPlayerCache.downloadCacheSingleton.get(context)
   * ```
   */
  val downloadCacheSingleton = Capsule(downloadCacheCreator)
}
