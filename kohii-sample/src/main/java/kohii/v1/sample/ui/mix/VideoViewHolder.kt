/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.mix

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.get
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Master
import kohii.media.MediaItem
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.common.inflateView
import kohii.v1.sample.data.DrmItem
import kohii.v1.sample.data.Item

/**
 * @author eneim (2018/07/06).
 */
class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Master
) : BaseViewHolder(
    parent,
    R.layout.holder_mix_view
) {

  private val mediaName = itemView.findViewById(R.id.videoTitle) as TextView
  private val playerContainer = itemView.findViewById(R.id.playerContainer) as FrameLayout

  var itemTag: String? = null

  override fun bind(item: Any?) {
    if (playerContainer[0] is PlayerView) playerContainer.removeViewAt(0)
    if (item is Item) {
      val drmItem = item.drmScheme?.let { DrmItem(item) }
      // Dynamically create the PlayerView instance.
      val playerView = (drmItem?.let {
        // Encrypted video must be played on SurfaceView.
        playerContainer.inflateView(R.layout.playerview_surface)
      } ?: playerContainer.inflateView(R.layout.playerview_texture)) as PlayerView
      playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
      playerContainer.addView(playerView, 0)

      val mediaItem = MediaItem(Uri.parse(item.uri), item.extension, drmItem)
      itemTag = "${javaClass.canonicalName}::${item.uri}::$adapterPosition"
      mediaName.text = item.name

      kohii.setUp(mediaItem)
          .with {
            tag = requireNotNull(itemTag)
            preload = false
            repeatMode = Player.REPEAT_MODE_ONE
          }
          .bind(playerView)
    }
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    itemTag = null
  }
}
