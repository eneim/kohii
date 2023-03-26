/*
 * Copyright (c) 2023 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.youtube

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubePlaylistPagingSource(
  youtube: YouTube,
  apiKey: String,
  playlistId: String
) : PagingSource<String, Video>() {

  private val playlistRequest = youtube.playlistItems()
    .list(listOf(YouTubeViewModel.YOUTUBE_PLAYLIST_PART))
    .setPlaylistId(playlistId)
    .setFields(YouTubeViewModel.YOUTUBE_PLAYLIST_FIELDS)
    .setMaxResults(YouTubeViewModel.YOUTUBE_PLAYLIST_MAX_RESULTS)
    .setKey(apiKey)

  private val videosRequest = youtube.videos()
    .list(listOf(YouTubeViewModel.YOUTUBE_VIDEOS_PART))
    .setFields(YouTubeViewModel.YOUTUBE_VIDEOS_FIELDS)
    .setKey(apiKey)

  override fun getRefreshKey(state: PagingState<String, Video>): String? = null

  override suspend fun load(params: LoadParams<String>): LoadResult<String, Video> {
    val (ids, prevKey, nextKey) = withContext(Dispatchers.IO) {
      val response = playlistRequest.setPageToken(params.key).execute()
      Triple(
        response.items
          .map { playlistItem -> playlistItem.snippet.resourceId.videoId }
          .toList(),
        response.prevPageToken,
        response.nextPageToken
      )
    }

    val videos = withContext(Dispatchers.IO) {
      videosRequest
        .setId(ids)
        .execute()
    }

    return LoadResult.Page(
      data = videos.items,
      prevKey = prevKey,
      nextKey = nextKey
    )
  }
}
