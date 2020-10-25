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

package kohii.v1.sample.tiktok.ui.home

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kohii.v1.core.Playback
import kohii.v1.sample.data.Video
import kohii.v1.sample.tiktok.databinding.HolderVerticalVideoBinding

class VideoViewHolder(
  val binding: HolderVerticalVideoBinding
) : ViewHolder(binding.root), Playback.StateListener {

  var videoItem: Video? = null
    set(value) {
      field = value
      if (value != null) {
        val firstPlaylist = value.playlist.first()
        videoThumbnail = firstPlaylist.image
        videoFile = firstPlaylist.sources
            .firstOrNull {
              it.file.endsWith("m3u8")
            }?.file ?: firstPlaylist.sources.first().file
      } else {
        videoThumbnail = null
        videoFile = null
      }
    }

  internal var videoFile: String? = null
  internal var videoThumbnail: String? = null

  override fun onBuffering(playback: Playback, playWhenReady: Boolean) {
    binding.status.text = "Buffering"
  }

  override fun onPlaying(playback: Playback) {
    binding.status.text = "Playing"
  }

  override fun onPaused(playback: Playback) {
    binding.status.text = "Paused"
  }

  override fun onError(playback: Playback, exception: Exception) {
    binding.status.text = "Error: ${exception.message}"
  }

  override fun onEnded(playback: Playback) {
    binding.status.text = "Ended"
  }
}
