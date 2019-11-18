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

package kohii.v1.sample.ui.overlay

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import kohii.core.Playback
import kohii.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.data.Video
import kotlin.properties.Delegates

internal class VideoItemHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  private val clickListener: OnClickListener
) : BaseViewHolder(inflater, R.layout.holder_video_text_overlay, parent),
    Playback.Callback,
    Playback.PlaybackListener,
    OnClickListener {

  override fun onClick(v: View?) {
    clickListener.onItemClick(v!!, null, adapterPosition, itemId, rebinder)
  }

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  internal val thumbnail = itemView.findViewById(R.id.videoImage) as ImageView
  internal val playerViewContainer = itemView.findViewById(R.id.playerViewContainer) as ViewGroup
  internal val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView

  init {
    itemView.findViewById<View>(R.id.playerContainer)
        .setOnClickListener(this)
  }

  internal var videoData: Video? by Delegates.observable<Video?>(
      initialValue = null,
      onChange = { _, _, value ->
        if (value != null) {
          val firstItem = value.playlist.first()
          videoImage = firstItem.image
          videoFile = firstItem.sources.first()
              .file
        } else {
          videoImage = null
          videoFile = null
        }
      }
  )

  internal var videoFile: String? = null
  internal var videoImage: String? = null
  internal val videoTag: String?
    get() = this.videoFile?.let { "$it::$adapterPosition" }

  // Trick
  internal val rebinder: Rebinder?
    get() = this.videoTag?.let { Rebinder(it) }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    this.videoData = null
    thumbnail.isVisible = true
  }

  override fun beforePlay(playback: Playback) {
    thumbnail.isVisible = false
  }

  override fun afterPause(playback: Playback) {
    thumbnail.isVisible = true
  }

  override fun onEnded(playback: Playback) {
    thumbnail.isVisible = true
  }

  override fun onInActive(playback: Playback) {
    thumbnail.isVisible = true
  }

  // Selection

  fun getItemDetails(): ItemDetails<Rebinder> {
    return object : ItemDetails<Rebinder>() {
      override fun getSelectionKey() = rebinder

      override fun getPosition() = adapterPosition
    }
  }
}
