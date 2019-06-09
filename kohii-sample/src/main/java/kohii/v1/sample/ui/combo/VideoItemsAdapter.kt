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

package kohii.v1.sample.ui.combo

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.MediaItem
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.ViewTarget
import kohii.v1.sample.R
import kohii.v1.sample.data.DrmItem
import kohii.v1.sample.data.Item

class VideoItemsAdapter(
  val kohii: Kohii,
  private val items: List<Item>,
  private val onClick: ((VideoViewHolder, Int) -> Unit)? = null,
  private val onLoad: ((VideoViewHolder, Int) -> Unit)? = null
) : Adapter<VideoViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): VideoViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.holder_player_container, parent, false)
    val holder = VideoViewHolder(view)
    holder.playerContainer.setOnClickListener {
      if (holder.adapterPosition >= 0) {
        onClick?.invoke(holder, holder.adapterPosition)
      }
    }
    return holder
  }

  override fun getItemCount() = Int.MAX_VALUE

  override fun onBindViewHolder(
    holder: VideoViewHolder,
    position: Int
  ) {
    val item = items[position % items.size]
    val drmItem = item.drmScheme?.let { DrmItem(item) }
    val mediaItem = MediaItem(Uri.parse(item.uri), item.extension, drmItem)
    val itemTag = "${javaClass.canonicalName}::${item.uri}::${holder.adapterPosition}"

    holder.videoTitle.text = item.name

    // Create a target that contains a thumbnail.
    val target = object : ViewTarget<ViewGroup, PlayerView>(holder.playerContainer) {
      override fun attachRenderer(renderer: PlayerView) {
        container.findViewById<ImageView>(R.id.thumbnail)
            .isVisible = false
        super.attachRenderer(renderer)
      }

      override fun detachRenderer(renderer: PlayerView): Boolean {
        container.findViewById<ImageView>(R.id.thumbnail)
            .isVisible = true
        return super.detachRenderer(renderer)
      }
    }

    holder.rebinder = kohii.setUp(mediaItem)
        .with {
          tag = itemTag
          preLoad = false
          repeatMode = Playable.REPEAT_MODE_ONE
        }
        .bind(target) {
          onLoad?.invoke(holder, position)
          it.addPlayerEventListener(object : PlayerEventListener {
            override fun onVideoSizeChanged(
              width: Int,
              height: Int,
              unappliedRotationDegrees: Int,
              pixelWidthHeightRatio: Float
            ) {
              holder.aspectRatio = width / height.toFloat()
              holder.playerContainer.setAspectRatio(holder.aspectRatio)
              it.removePlayerEventListener(this)
            }
          })
        }
  }
}
