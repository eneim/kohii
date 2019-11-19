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

package kohii.v1.sample.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Common
import kohii.v1.core.Master
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.InitData
import kohii.v1.sample.ui.list.data.Item

/**
 * @author eneim (2018/07/06).
 */
class VideoViewHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  private val kohii: Master,
  private val listener: OnClickListener
) : BaseViewHolder(inflater, R.layout.holder_player_view, parent),
    View.OnClickListener, Playback.Callback {

  init {
    itemView.setOnClickListener(this)
  }

  val playerView = itemView.findViewById(R.id.playerView) as PlayerView
  val playerContainer = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
  val transView = playerView.findViewById(R.id.exo_content_frame) as View

  var rebinder: Rebinder? = null
  var playback: Playback? = null
  var payload: InitData? = null

  override fun bind(item: Item?) {
    if (item != null) {
      val itemTag = "${javaClass.canonicalName}::${item.content}::$adapterPosition"
      payload = InitData(tag = itemTag, aspectRatio = item.width / item.height.toFloat())
      playerContainer.setAspectRatio(payload!!.aspectRatio)
      rebinder = kohii.setUp(item.content) {
        tag = itemTag
        preload = true
        repeatMode = Common.REPEAT_MODE_ONE
        callbacks = arrayOf(this@VideoViewHolder)
      }
          .bind(playerView) {
            playback = it
            listener.onItemLoaded(itemView, adapterPosition)
          }

      ViewCompat.setTransitionName(transView, itemTag)
    }
  }

  override fun onClick(v: View?) {
    if (v != null && payload != null && rebinder != null) {
      listener.onItemClick(
          v, transView, adapterPosition, Pair(rebinder!!, payload!!)
      )
    }
  }
}
