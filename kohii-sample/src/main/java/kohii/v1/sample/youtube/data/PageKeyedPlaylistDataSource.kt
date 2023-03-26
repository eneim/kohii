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

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.Video
import java.util.concurrent.Executor

// Merely adopt: https://github.com/googlesamples/android-architecture-components/blob/master/PagingWithNetworkSample/app/
// src/main/java/com/android/example/paging/pagingwithnetwork/reddit/repository/inMemory/byPage/PageKeyedSubredditDataSource.kt
class PageKeyedPlaylistDataSource(
  private val apiKey: String,
  val youtube: YouTube,
  @Suppress("CanBeParameter") val playlistId: String,
  @Suppress("MemberVisibilityCanBePrivate") val executor: Executor,
  @Suppress("CanBeParameter") val pageSize: Long
) : PageKeyedDataSource<String, Video>() {

  // keep a function reference for the retry event
  private var retry: (() -> Any)? = null

  /**
   * There is no sync on the state because paging will always call loadInitial first then wait
   * for it to return some success value before calling loadAfter.
   */
  val networkState = MutableLiveData<NetworkState>()

  val initialLoad = MutableLiveData<NetworkState>()

  private val request = youtube.playlistItems()
    .list(listOf(YouTubeDataSourceFactory.YOUTUBE_PLAYLIST_PART))
    .setPlaylistId(playlistId)
    .setFields(YouTubeDataSourceFactory.YOUTUBE_PLAYLIST_FIELDS)
    .setMaxResults(pageSize)
    .setKey(apiKey)

  fun retryAllFailed() {
    val prevRetry = retry
    retry = null
    prevRetry?.let {
      executor.execute {
        it.invoke()
      }
    }
  }

  override fun loadInitial(
    params: LoadInitialParams<String>,
    callback: LoadInitialCallback<String, Video>
  ) {
    networkState.postValue(NetworkState.LOADING)
    initialLoad.postValue(NetworkState.LOADING)

    val result = request.setPageToken(null)
      .execute()
    val videoIds = result.items.map<PlaylistItem, String> { it.snippet.resourceId.videoId }
      .toList()
    val videos = youtube.videos()
      .list(listOf(YouTubeDataSourceFactory.YOUTUBE_VIDEOS_PART))
      .setFields(YouTubeDataSourceFactory.YOUTUBE_VIDEOS_FIELDS)
      .setKey(apiKey)
      .setId(videoIds)
      .execute()

    callback.onResult(videos.items, result.prevPageToken, result.nextPageToken)
    networkState.postValue(NetworkState.LOADED)
    initialLoad.postValue(NetworkState.LOADED)
  }

  override fun loadAfter(
    params: LoadParams<String>,
    callback: LoadCallback<String, Video>
  ) {
    networkState.postValue(NetworkState.LOADING)

    val result = request.setPageToken(params.key)
      .execute()
    val videoIds = result.items.map<PlaylistItem, String> { it.snippet.resourceId.videoId }
      .toList()
    val videos = youtube.videos()
      .list(listOf(YouTubeDataSourceFactory.YOUTUBE_VIDEOS_PART))
      .setFields(YouTubeDataSourceFactory.YOUTUBE_VIDEOS_FIELDS)
      .setKey(apiKey)
      .setId(videoIds)
      .execute()

    callback.onResult(videos.items, result.nextPageToken)
    networkState.postValue(NetworkState.LOADED)
  }

  override fun loadBefore(
    params: LoadParams<String>,
    callback: LoadCallback<String, Video>
  ) {
    /* do nothing */
  }
}
