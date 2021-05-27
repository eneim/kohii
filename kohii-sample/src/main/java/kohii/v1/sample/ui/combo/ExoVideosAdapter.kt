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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.Player
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Callback
import kohii.v1.exoplayer.Kohii
import kohii.v1.media.MediaItem
import kohii.v1.sample.data.DrmItem
import kohii.v1.sample.data.Item

class ExoVideosAdapter(
  val kohii: Kohii,
  private val items: List<Item>,
  private val onClick: ((ExoVideoHolder, Int) -> Unit)? = null,
  private val onLoad: ((ExoVideoHolder, Int) -> Unit)? = null
) : Adapter<ExoVideoHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ExoVideoHolder {
    val holder = ExoVideoHolder(parent)
    holder.container.setOnClickListener {
      if (holder.adapterPosition >= 0) {
        onClick?.invoke(holder, holder.adapterPosition)
      }
    }
    return holder
  }

  override fun getItemCount() = Int.MAX_VALUE / 2

  override fun onBindViewHolder(
    holder: ExoVideoHolder,
    position: Int
  ) {
    val item = items[position % items.size]
    holder.videoTitle.text = item.name

    val drmItem = item.drmScheme?.let { DrmItem(item) }
    val mediaItem =
      MediaItem(Uri.parse(item.uri), item.extension, drmItem)
    val itemTag = "${javaClass.canonicalName}::${item.uri}::${holder.adapterPosition}"

    holder.rebinder = kohii.setUp(mediaItem) {
      tag = itemTag
      // preLoad = false
      repeatMode = Player.REPEAT_MODE_ONE
      callbacks += object : Callback {
        override fun onRemoved(playback: Playback) {
          playback.removeStateListener(holder)
        }
      }
      artworkHintListener = holder
    }
        .bind(holder.container) {
          onLoad?.invoke(holder, position)
          it.addStateListener(holder)
        }
  }
}
