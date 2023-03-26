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

package kohii.v1.sample.ui.youtube2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.DemoApp
import kohii.v1.sample.youtube.data.YouTubePlaylistRepository
import java.util.concurrent.Executors

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    // Android Summit 2019
    const val YOUTUBE_PLAYLIST_ID = "PLWz5rJ2EKKc_xXXubDti2eRnIKU0p7wHd"
    const val YOUTUBE_PLAYLIST_MAX_RESULTS = 20L
  }

  private val jsonFactory = GsonFactory.getDefaultInstance()
  private val httpTransport = NetHttpTransport()
  val youtube: YouTube = YouTube.Builder(httpTransport, jsonFactory, null)
    .setApplicationName("Kohii + Youtube, " + BuildConfig.VERSION_NAME)
    .build()

  private val repository =
    YouTubePlaylistRepository(
      apiKey = (application as DemoApp).youtubeApiKey,
      youtube = youtube,
      executor = Executors.newSingleThreadExecutor()
    )

  private val playlistId = MutableLiveData<String>()
  private val repoResult = playlistId.map {
    repository.itemsOfPlaylist(it, YOUTUBE_PLAYLIST_MAX_RESULTS)
  }

  val posts = repoResult.switchMap { it.pagedList }
  val networkState = repoResult.switchMap { it.networkState }
  val refreshState = repoResult.switchMap { it.refreshState }

  fun loadPlaylist(playlistId: String): Boolean {
    if (this.playlistId.value == playlistId) {
      return false
    }
    this.playlistId.value = playlistId
    return true
  }
}
