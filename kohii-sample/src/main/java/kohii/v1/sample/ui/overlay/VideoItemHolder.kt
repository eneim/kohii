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
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.data.Video
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate")
internal class VideoItemHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  private val clickListener: OnClickListener
) : BaseViewHolder(inflater, R.layout.holder_video_text_overlay, parent),
    Playback.ArtworkHintListener,
    OnClickListener {

  override fun onClick(v: View?) {
    clickListener.onItemClick(v!!, null, adapterPosition, itemId, rebinder)
  }

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  internal val thumbnail = itemView.findViewById(R.id.videoImage) as ImageView
  internal val playerViewContainer = itemView.findViewById(R.id.playerViewContainer) as ViewGroup
  internal val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView
  internal val container = itemView.findViewById(R.id.playerContainer) as View

  internal var videoData by Delegates.observable<Video?>(
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

  override fun onRecycled() {
    super.onRecycled()
    this.videoData = null
    thumbnail.isVisible = true
  }

  override fun clearTransientStates() {
    super.clearTransientStates()
    thumbnail.clearAnimation()
  }

  override fun onAttached() {
    super.onAttached()
    container.setOnClickListener(this)
  }

  override fun onDetached() {
    super.onDetached()
    container.setOnClickListener(null)
  }

  override fun onArtworkHint(
    playback: Playback,
    shouldShow: Boolean,
    position: Long,
    state: Int
  ) {
    if (!shouldShow) {
      thumbnail.animate()
          .alpha(0F)
          .setDuration(200)
          .withEndAction {
            thumbnail.isVisible = false
          }
          .start()
    } else {
      thumbnail.alpha = 1F
      thumbnail.animate()
          .alpha(1F)
          .setDuration(0)
          .withEndAction {
            thumbnail.isVisible = true
          }
          .start()
    }
  }

  override fun toString(): String {
    return "[$itemView], $playerViewContainer"
  }

  // Selection

  fun getItemDetails(): ItemDetails<Rebinder> {
    return object : ItemDetails<Rebinder>() {
      override fun getSelectionKey() = rebinder

      override fun getPosition() = adapterPosition
    }
  }
}
