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

package kohii.v1.sample.ui.nested5

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import kohii.core.Common
import kohii.core.Master
import kohii.core.Playback
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.data.Item

/**
 * @author eneim (2018/07/06).
 */
@Suppress("MemberVisibilityCanBePrivate")
class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Master
) : BaseViewHolder(parent, R.layout.holder_player_view_horizontal), Playback.Callback {

  val mediaName: TextView = itemView.findViewById(R.id.videoTitle)
  val playerContainer: FrameLayout = itemView.findViewById(R.id.playerContainer)

  var itemTag: String? = null

  override fun bind(item: Any?) {
    if (item is Pair<*, *>) {
      val (parentPosition, videoItem) = item
      if (videoItem is Item) {
        // val mediaItem = MediaItem(Uri.parse(item.uri), item.extension, drmItem)
        itemTag = "NEST::$parentPosition::${videoItem.uri}::$adapterPosition"
        mediaName.text = videoItem.name

        kohii.setUp(assetVideoUri) {
          tag = requireNotNull(itemTag)
          preload = false
          repeatMode = Common.REPEAT_MODE_ONE
          callbacks = arrayOf(this@VideoViewHolder)
        }
            .bind(playerContainer)
      }
    }
  }
}
