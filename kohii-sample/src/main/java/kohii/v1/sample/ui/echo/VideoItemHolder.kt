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
import androidx.core.util.contains
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.button.MaterialButton
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.ViewTarget
import kohii.v1.VolumeChangedListener
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.data.Sources
import kohii.v1.sample.data.Video
import kohii.v1.sample.svg.GlideApp

@Suppress("MemberVisibilityCanBePrivate")
class VideoItemHolder(
  parent: ViewGroup,
  private val kohii: Kohii,
  private val viewModel: VolumeStateVideoModel
) : BaseViewHolder(parent, R.layout.holder_video_text_overlay), VolumeChangedListener {

  @SuppressLint("SetTextI18n")
  override fun onVolumeChanged(volumeInfo: VolumeInfo) {
    volumeButton.text = "Mute: ${volumeInfo.mute}"
    viewModel.saveVolumeInfo(this.adapterPosition, volumeInfo)
  }

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView
  val videoImage = itemView.findViewById(R.id.videoImage) as ImageView
  // val playerViewContainer = itemView.findViewById(R.id.playerViewContainer) as ViewGroup
  val playerContainer = itemView.findViewById(R.id.playerContainer) as ViewGroup
  val volumeButton = itemView.findViewById(R.id.volumeButton) as MaterialButton

  init {
    volumeButton.isVisible = true
  }

  var videoSources: Sources? = null
  var playback: Playback<PlayerView>? = null

  val tagKey: String?
    get() = this.videoSources?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  @SuppressLint("SetTextI18n")
  override fun bind(item: Any?) {
    (item as? Video)?.also {
      videoTitle.text = it.title
      videoInfo.text = it.description
      this.videoSources = it.playlist.first()
          .also { pl ->
            GlideApp.with(itemView)
                .load(pl.image)
                .into(videoImage)
          }
          .sources.first()

      kohii.setUp(videoSources!!.file)
          .with {
            tag = tagKey
            repeatMode = Playable.REPEAT_MODE_ONE
          }
          .bind(ViewTarget(playerContainer)) { playback ->
            playback.addVolumeChangeListener(this@VideoItemHolder)
            if (viewModel.volumeInfoStore.contains(this.adapterPosition)) {
              playback.volumeInfo = viewModel.volumeInfoStore.get(this.adapterPosition)
            } else {
              viewModel.saveVolumeInfo(this.adapterPosition, playback.volumeInfo) // save first.
            }
            volumeButton.text = "Mute: ${playback.volumeInfo.mute}"
            this@VideoItemHolder.playback = playback
          }
    }
  }

  override fun onRecycled(success: Boolean) {
    playback?.removeVolumeChangeListener(this)
    playback = null
  }
}
