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

package kohii.v1.sample.youtube.data

import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import java.util.concurrent.Executor

class YouTubePlaylistRepository(
  @Suppress("MemberVisibilityCanBePrivate")
  val apiKey: String,
  val youtube: YouTube,
  private val executor: Executor
) {
  fun itemsOfPlaylist(
    playlistId: String,
    pageSize: Long
  ): Listing<Video> {
    val sourceFactory = YouTubeDataSourceFactory(
        apiKey, youtube, playlistId, executor, pageSize
    )
    // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
    val livePagedList = sourceFactory.toLiveData(
        pageSize = pageSize.toInt(),
        // provide custom executor for network requests, otherwise it will default to
        // Arch Components' IO keyToPool which is also used for disk access
        fetchExecutor = executor
    )
    val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
      it.initialLoad
    }
    return Listing(
        pagedList = livePagedList,
        networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
          it.networkState
        },
        retry = {
          sourceFactory.sourceLiveData.value?.retryAllFailed()
        },
        refresh = {
          sourceFactory.sourceLiveData.value?.invalidate()
        },
        refreshState = refreshState
    )
  }
}
