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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.PlayableCreator
import kohii.v1.sample.R

class YouTubeItemsAdapter(
  private val creator: PlayableCreator<*>
) : Adapter<YouTubeViewHolder>() {

  companion object {
    val videos = arrayOf(
        "_pOd4uW2upg", "EOjq4OIWKqM", "FV3iN4PIB5U", "NNWejxBORgc", "0-HpOUwbp5w", "lRiYvQbKoiY"
    )

    val videoUrls = arrayOf(
        "https://content.jwplatform.com/videos/0G7vaSoF-oQOe5Prq.mp4",
        "https://content.jwplatform.com/videos/0G7vaSoF-oQOe5Prq.mp4",
        "https://content.jwplatform.com/videos/0G7vaSoF-oQOe5Prq.mp4",
        "https://content.jwplatform.com/videos/0G7vaSoF-oQOe5Prq.mp4"
    )
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): YouTubeViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.holder_youtube_container, parent, false)
    return YouTubeViewHolder(view)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: YouTubeViewHolder,
    position: Int
  ) {
    creator.setUp(getMedia(position))
        .with { threshold = 0.99F }
        .bind(holder.container)
  }

  private fun getMedia(position: Int): String {
    return videos[position % videos.size]
  }
}
