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

package kohii.v1.sample.youtube

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.R
import kohii.v1.sample.youtube.data.YouTubePlaylistRepository
import java.util.concurrent.Executors

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {

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

  private val jsonFactory = GsonFactory.getDefaultInstance()
  private val httpTransport = NetHttpTransport()
  val youtube: YouTube = YouTube.Builder(httpTransport, jsonFactory, null)
      .setApplicationName("Kohii + Youtube, " + BuildConfig.VERSION_NAME)
      .build()

  private val apiKey = application.getString(R.string.yt_api_key)
  private val repository =
    YouTubePlaylistRepository(
        apiKey, youtube, Executors.newSingleThreadExecutor()
    )

  private val playlistId = MutableLiveData<String>()
  private val repoResult = map(playlistId) {
    repository.itemsOfPlaylist(it, YOUTUBE_PLAYLIST_MAX_RESULTS)
  }

  val posts = switchMap(repoResult) { it.pagedList }
  val networkState = switchMap(repoResult) { it.networkState }
  val refreshState = switchMap(repoResult) { it.refreshState }

  fun loadPlaylist(playlistId: String): Boolean {
    if (this.playlistId.value == playlistId) {
      return false
    }
    this.playlistId.value = playlistId
    return true
  }
}
