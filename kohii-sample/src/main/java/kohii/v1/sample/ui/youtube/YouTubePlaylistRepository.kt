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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import kotlinx.coroutines.flow.Flow

class YouTubePlaylistRepository(
  private val apiKey: String,
  private val youtube: YouTube
) {

  fun playlist(id: String): Flow<PagingData<Video>> = Pager(
    config = PagingConfig(pageSize = 20),
    initialKey = null,
    pagingSourceFactory = {
      YouTubePlaylistPagingSource(
        youtube = youtube,
        apiKey = apiKey,
        playlistId = id
      )
    }
  )
    .flow
}
