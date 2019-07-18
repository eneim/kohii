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

package kohii.v1.sample.ui.nested

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.get
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.MediaItem
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.sample.R

/**
 * @author eneim (2018/07/06).
 */
class VideoViewHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  val kohii: Kohii
) : BaseViewHolder(
    inflater,
    R.layout.holder_player_view_horizontal,
    parent
), Playback.Callback {

  val mediaName = itemView.findViewById(R.id.videoTitle) as TextView
  val playerContainer = itemView.findViewById(R.id.playerContainer) as FrameLayout

  var itemTag: String? = null
  var playback: Playback<PlayerView>? = null

  @SuppressLint("SetTextI18n")
  override fun bind(item: Item?) {
    if (playerContainer[0] is PlayerView) playerContainer.removeViewAt(0)

    if (item != null) {
      val drmItem = item.drmScheme?.let { DrmItem(item) }
      // Dynamically create the PlayerView instance.
      val playerView = (drmItem?.let {
        // Encrypted video must be played on SurfaceView.
        inflater.inflate(R.layout.playerview_surface, playerContainer, false)
      } ?: inflater.inflate(R.layout.playerview_texture, playerContainer, false)) as PlayerView
      playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
      playerContainer.addView(playerView, 0)

      val mediaItem = MediaItem(Uri.parse(item.uri), item.extension, drmItem)
      itemTag = "${javaClass.canonicalName}::${item.uri}::$adapterPosition"
      mediaName.text = item.name

      kohii.setUp(mediaItem)
          .with {
            tag = itemTag
            preLoad = false
            repeatMode = Playable.REPEAT_MODE_ONE
            callback = this@VideoViewHolder
          }
          .bind(playerView) {
            playback = it
          }
    }
  }
}
