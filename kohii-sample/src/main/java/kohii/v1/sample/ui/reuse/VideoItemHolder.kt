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

package kohii.v1.sample.ui.reuse

import android.graphics.Point
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.contains
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.sample.R
import kohii.v1.sample.svg.GlideApp
import kohii.v1.sample.ui.reuse.data.Sources
import kohii.v1.sample.ui.reuse.data.Video

@Suppress("MemberVisibilityCanBePrivate")
internal class VideoItemHolder(
  inflater: LayoutInflater,
  layoutRes: Int,
  parent: ViewGroup,
  private val kohii: Kohii
) : BaseViewHolder(inflater, layoutRes, parent),
    Playback.Callback,
    PlaybackEventListener {

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView
  val videoImage = itemView.findViewById(R.id.videoImage) as ImageView
  val playerContainer = itemView.findViewById(R.id.playerContainer) as ViewGroup

  var playable: Playable<PlayerView>? = null
  var playback: Playback<PlayerView>? = null
  var videoSources: Sources? = null

  val tagKey: String?
    get() = this.videoSources?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  override fun bind(item: Any?) {
    (item as? Video)?.let {
      videoTitle.text = it.title
      videoInfo.text = it.description
      this.videoSources = it.playlist.first()
          .also { pl ->
            GlideApp.with(itemView)
                .load(pl.image)
                .into(videoImage)
          }
          .sources.first()

      this.playable = kohii.setUp(videoSources!!.file)
          .copy(tag = tagKey, repeatMode = Playable.REPEAT_MODE_ONE)
          .asPlayable()
    }
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    this.playback?.apply {
      removePlaybackEventListener(this@VideoItemHolder)
      removeCallback(this@VideoItemHolder)
    }
    videoImage.isVisible = true
  }

  override fun beforePlay() {
    videoImage.isVisible = false
  }

  override fun onPlaying() {
    videoImage.isVisible = false
  }

  override fun afterPause() {
    videoImage.isVisible = true
  }

  override fun onCompleted() {
    videoImage.isVisible = true
  }

  override fun onInActive(playback: Playback<*>) {
    videoImage.isVisible = true
  }

  fun bindView(playerView: PlayerView) {
    playerContainer.addView(playerView, 0)
    this.playable?.bind(playerView) {
      it.addPlaybackEventListener(this)
      it.addCallback(this)
      this.playback = it
    }
  }

  // Also remove playerView
  fun unbindView(playerView: PlayerView?) {
    this.playback?.unbind()
    if (playerView != null && playerContainer.contains(playerView)) {
      playerContainer.removeView(playerView)
    }
    this.playback = null
  }

  fun wantsToPlay(): Boolean {
    itemView.parent ?: return false
    val target = playerContainer
    val playerRect = Rect()
    val visible = target.getGlobalVisibleRect(playerRect, Point())
    if (!visible) return false

    val drawRect = Rect()
    target.getDrawingRect(drawRect)
    val drawArea = drawRect.width() * drawRect.height()

    var offset = 0f
    if (drawArea > 0) {
      val visibleArea = playerRect.height() * playerRect.width()
      offset = visibleArea / drawArea.toFloat()
    }
    return offset >= 0.85f
  }
}