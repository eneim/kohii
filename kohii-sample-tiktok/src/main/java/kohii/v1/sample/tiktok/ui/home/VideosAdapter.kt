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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import coil.api.load
import com.google.android.exoplayer2.Player
import kohii.v1.core.Playback
import kohii.v1.core.Playback.ArtworkHintListener
import kohii.v1.core.controller
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.data.Video
import kohii.v1.sample.tiktok.R
import kohii.v1.sample.tiktok.databinding.HolderVerticalVideoBinding

class VideosAdapter(
  private val videos: List<Video>,
  val kohii: Kohii
) : Adapter<VideoViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
    val binding: HolderVerticalVideoBinding =
      HolderVerticalVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return VideoViewHolder(binding)
  }

  override fun getItemCount(): Int = Int.MAX_VALUE / 2

  override fun onBindViewHolder(holder: VideoViewHolder, pos: Int) {
    holder.videoItem = videos[pos % videos.size]
    val videoFile = requireNotNull(holder.videoFile)
    kohii.setUp(videoFile) {
      tag = "video::$pos"
      threshold = 0.5F
      preload = true
      repeatMode = Player.REPEAT_MODE_ONE
      artworkHintListener = object : ArtworkHintListener {
        override fun onArtworkHint(
          playback: Playback,
          shouldShow: Boolean,
          position: Long,
          state: Int
        ) {
          holder.binding.thumbnailContainer.isVisible = playback.playable?.isPlaying() == false
        }
      }

      controller = controller(kohiiCanStart = true, kohiiCanPause = true) { playback, _ ->
        val playable = playback.playable ?: return@controller
        holder.binding.container.setOnClickListener {
          if (playable.isPlaying()) playback.manager.pause(playable)
          else playback.manager.play(playable)
        }
      }
    }.bind(holder.binding.playerView) {
      it.addStateListener(holder)
    }

    val videoStaticThumb = holder.videoThumbnail ?: return
    holder.binding.thumbnail.load(videoStaticThumb) {
      crossfade(true)
      placeholder(R.drawable.exo_edit_mode_logo)
      error(R.drawable.exo_edit_mode_logo)
    }
  }

  override fun onViewRecycled(holder: VideoViewHolder) {
    super.onViewRecycled(holder)
    holder.videoItem = null
    kohii.cancel(holder.binding.playerView)
  }

  override fun onFailedToRecycleView(holder: VideoViewHolder): Boolean {
    holder.videoItem = null
    kohii.cancel(holder.binding.playerView)
    return super.onFailedToRecycleView(holder)
  }
}
