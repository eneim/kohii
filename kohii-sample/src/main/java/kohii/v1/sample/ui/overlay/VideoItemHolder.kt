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

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlayableBinder
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.svg.GlideApp
import kohii.v1.sample.ui.overlay.data.Sources
import kohii.v1.sample.ui.overlay.data.Video

@Suppress("MemberVisibilityCanBePrivate")
internal class VideoItemHolder(
  inflater: LayoutInflater,
  layoutRes: Int,
  parent: ViewGroup,
  private val clickListener: OnClickListener,
  private val kohii: Kohii,
  private val host: VideoItemsAdapter
) : BaseViewHolder(inflater, layoutRes, parent),
    Playback.Callback,
    PlaybackEventListener,
    OnClickListener {

  override fun onClick(v: View?) {
    clickListener.onItemClick(v!!, null, adapterPosition, itemId, rebinder)
  }

  val videoTitle = itemView.findViewById(R.id.videoTitle) as TextView
  val videoInfo = itemView.findViewById(R.id.videoInfo) as TextView
  val videoImage = itemView.findViewById(R.id.videoImage) as ImageView
  val playerView = itemView.findViewById(R.id.playerView) as ViewGroup
  val playerContainer = itemView.findViewById(R.id.playerContainer) as View

  var playback: Playback<ViewGroup, PlayerView>? = null
  var binder: PlayableBinder<PlayerView>? = null
  var videoSources: Sources? = null

  init {
    itemView.findViewById<View>(R.id.playerContainer)
        .setOnClickListener(this)
  }

  val tagKey: String?
    get() = this.videoSources?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  // Trick
  val rebinder: Rebinder?
    get() = this.videoSources?.let { Rebinder(tagKey, PlayerView::class.java) }

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

      this.binder = kohii.setUp(videoSources!!.file)
          .config {
            Playable.Config(tag = tagKey, repeatMode = Playable.REPEAT_MODE_ONE)
          }

      if (host.selectionTracker?.isSelected(rebinder) == true) {
        this.playback = null
      } else {
        this.binder!!.bind(playerView) { pk ->
          pk.addPlaybackEventListener(this@VideoItemHolder)
          pk.addCallback(this@VideoItemHolder)
          this@VideoItemHolder.playback = pk
        }
      }
    }
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    this.videoSources = null
    this.playback?.apply {
      removeCallback(this@VideoItemHolder)
    }
    videoImage.isVisible = true
  }

  override fun beforePlay() {
    videoImage.isVisible = false
    Log.e("Kohii:VH", "beforePlay: $playback, $adapterPosition")
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

  override fun onInActive(playback: Playback<*, *>) {
    videoImage.isVisible = true
  }

  // Selection

  fun getItemDetails(): ItemDetails<Rebinder> {
    return object : ItemDetails<Rebinder>() {
      override fun getSelectionKey() = rebinder

      override fun getPosition() = adapterPosition
    }
  }
}
