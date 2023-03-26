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
import androidx.paging.DataSource
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import java.util.concurrent.Executor

class YouTubeDataSourceFactory(
  private val apiKey: String,
  val youtube: YouTube,
  private val playlistId: String,
  private val executor: Executor,
  private val pageSize: Long
) : DataSource.Factory<String, Video>() {

  companion object {
    // Android & Play at Google I/O 2019
    const val YOUTUBE_PLAYLIST_ID = "PLWz5rJ2EKKc9FfSQIRXEWyWpHD6TtwxMM"
    const val YOUTUBE_PLAYLIST_MAX_RESULTS = 20L

    // see: https://developers.google.com/youtube/v3/docs/playlistItems/list
    const val YOUTUBE_PLAYLIST_PART = "snippet"
    const val YOUTUBE_PLAYLIST_FIELDS =
      "pageInfo,nextPageToken,items(id,snippet(resourceId/videoId))"

    // see: https://developers.google.com/youtube/v3/docs/videos/list
    const val YOUTUBE_VIDEOS_PART = "snippet,contentDetails"

    // video resource properties that the response will include.
    const val YOUTUBE_VIDEOS_FIELDS =
      "items(id,snippet(title,description,thumbnails/medium,thumbnails/maxres,channelTitle))"
    // selector specifying which fields to include in a partial response.
  }

  val sourceLiveData = MutableLiveData<PageKeyedPlaylistDataSource>()
  override fun create(): DataSource<String, Video> {
    val source = PageKeyedPlaylistDataSource(apiKey, youtube, playlistId, executor, pageSize)
    sourceLiveData.postValue(source)
    return source
  }
}
