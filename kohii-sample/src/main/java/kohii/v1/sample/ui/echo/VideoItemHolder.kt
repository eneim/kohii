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

package kohii.v1.sample.ui.echo

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import kohii.v1.core.Playback
import kohii.v1.core.Scope
import kohii.v1.exoplayer.Kohii
import kohii.v1.media.VolumeInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.data.Video
import kohii.v1.sample.svg.GlideApp
import kotlin.properties.Delegates

@SuppressLint("SetTextI18n")
@Suppress("MemberVisibilityCanBePrivate")
class VideoItemHolder(
  parent: ViewGroup,
  private val kohii: Kohii
) : BaseViewHolder(parent, R.layout.holder_video_text_overlay) {

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView
  val videoImage = itemView.findViewById(R.id.videoImage) as ImageView
  val playerViewContainer = itemView.findViewById(R.id.playerViewContainer) as ViewGroup
  val playerContainer = itemView.findViewById(R.id.playerContainer) as ViewGroup
  val volumeButton = itemView.findViewById(R.id.volumeButton) as MaterialButton

  init {
    volumeButton.isVisible = true
  }

  internal var playback: Playback? = null

  private var rawData: Video? by Delegates.observable<Video?>(null,
      onChange = { _, _, newVal ->
        if (newVal != null) {
          val playlist = newVal.playlist.first()
          this.videoItem = VideoItem(
              newVal.title,
              newVal.description,
              playlist.image,
              playlist.sources.first().file
          )
        } else {
          this.videoItem = null
        }
      })

  private var videoItem by Delegates.observable<VideoItem?>(null,
      onChange = { _, oldVal, newVal ->
        if (newVal == oldVal) return@observable
        if (newVal != null) {
          videoTitle.text = newVal.title
          videoInfo.text = newVal.description
          GlideApp.with(itemView)
              .load(newVal.imageUrl)
              .into(videoImage)

          kohii.setUp(newVal.file) {
            tag = requireNotNull(tagKey)
          }
              .bind(playerViewContainer) { playback ->
                this@VideoItemHolder.playback = playback
                volumeInfo?.let { kohii.applyVolumeInfo(it, playback, Scope.PLAYBACK) }
              }
        } else {
          this.playback = null
        }
      })

  private val tagKey: String?
    get() = this.videoItem?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  private var volumeInfo by Delegates.observable<VolumeInfo?>(null,
      onChange = { _, _, to ->
        if (to != null) {
          playback?.let {
            kohii.applyVolumeInfo(to, it, Scope.PLAYBACK)
          }
          volumeButton.text = "Mute: ${to.mute}"
        }
      })

  internal fun applyVideoData(video: Video?) {
    this.rawData = video
  }

  internal fun applyVolumeInfo(volumeInfo: VolumeInfo) {
    this.volumeInfo = volumeInfo
  }

  override fun onRecycled(success: Boolean) {
    this.rawData = null
    this.volumeInfo = null
  }
}
