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

package kohii.v1.sample.ui.fbook.vh

import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import kohii.v1.core.Binder.Options
import kohii.v1.core.Common
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Controller
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.data.Sources
import kohii.v1.sample.data.Video

internal class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Kohii,
  val shouldBind: (Rebinder?) -> Boolean
) : FbookItemHolder(parent),
    Playback.StateListener,
    Playback.ArtworkHintListener {

  init {
    videoContainer.isVisible = true
  }

  internal val playerView = itemView.findViewById(R.id.playerView) as ViewGroup
  internal val volume = itemView.findViewById(R.id.volumeSwitch) as ImageButton
  internal val thumbnail = itemView.findViewById(R.id.thumbnail) as ImageView
  internal val playAgain = itemView.findViewById(R.id.playerAgain) as Button

  internal var playback: Playback? = null

  private var video: Video? = null
  private var videoImage: String? = null
  private var videoSources: Sources? = null

  private val videoTag: String?
    get() = this.videoSources?.let { "FB::${it.file}::$adapterPosition" }

  private val params: Options.() -> Unit
    get() = {
      tag = requireNotNull(videoTag)
      artworkHintListener = this@VideoViewHolder
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true
      }
    }

  // Trick here: we do not rely on the actual binding to have the Rebinder. This instance will
  // be useful in some verifications.
  internal val rebinder: Rebinder?
    get() = this.videoTag?.let { Rebinder(it) }

  override fun bind(item: Any?) {
    super.bind(item)
    (item as? Video)?.also {
      this.video = it
      this.videoSources = it.playlist.first()
          .also { pl ->
            videoImage = pl.image
            Glide.with(itemView)
                .load(pl.image)
                .into(thumbnail)
          }
          .sources.first()

      if (shouldBind(this.rebinder)) {
        kohii.setUp(assetVideoUri, params)
            .bind(playerView) { pk ->
              volume.isSelected = !pk.volumeInfo.mute
              pk.addStateListener(this@VideoViewHolder)
              playback = pk
            }
      }
    }
  }

  override fun onArtworkHint(
    shouldShow: Boolean,
    position: Long,
    state: Int
  ) {
    thumbnail.isVisible = shouldShow
    playAgain.isVisible = shouldShow && state == Common.STATE_ENDED
  }

  override fun onRecycled(success: Boolean) {
    video = null
    videoSources = null
    videoImage = null
    playback = null
  }
}
