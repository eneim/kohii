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

package kohii.v1.sample.ui.youtube2

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.api.services.youtube.model.Video
import kohii.v1.core.Playback
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.svg.GlideApp

class YouTubeViewHolder(
  parent: ViewGroup,
  layoutId: Int
) : BaseViewHolder(parent, layoutId), Playback.ArtworkHintListener {

  val container = itemView.findViewById(R.id.container) as FrameLayout
  val thumbnail = itemView.findViewById(R.id.thumbnail) as ImageView
  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView

  var playback: Playback? = null

  init {
    // thumbnail.isVisible = false
  }

  @SuppressLint("SetTextI18n")
  override fun bind(item: Any?) {
    super.bind(item)
    (item as? Video)?.apply {
      val lowResThumb = this.snippet.thumbnails.medium.url
      val thumbRequest = Glide.with(itemView)
          .load(lowResThumb)
      val highResThumb = this.snippet.thumbnails.maxres.url
      GlideApp.with(itemView)
          .load(highResThumb)
          .thumbnail(thumbRequest)
          .fitCenter()
          .into(thumbnail)

      videoTitle.text = "${this.snippet.title} - id: $id"
    }
  }

  override fun onArtworkHint(
    playback: Playback,
    shouldShow: Boolean,
    position: Long,
    state: Int
  ) {
    thumbnail.isVisible = shouldShow
    Log.d("Kohii::Art", "${playback?.tag} art: $shouldShow, $position, $state")
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    playback = null
  }
}
