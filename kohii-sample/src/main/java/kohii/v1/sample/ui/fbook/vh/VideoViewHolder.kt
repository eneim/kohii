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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Binder
import kohii.v1.Binder.Params
import kohii.v1.HeadlessPlaybackParams
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.PlaybackManager
import kohii.v1.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.data.Sources
import kohii.v1.sample.data.Video
import kotlin.LazyThreadSafetyMode.NONE

internal class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Kohii,
  val manager: PlaybackManager,
  val shouldBind: (Rebinder<*>?) -> Boolean
) : FbookItemHolder(parent), PlaybackEventListener {

  init {
    videoContainer.isVisible = true
  }

  internal val playerView = itemView.findViewById(R.id.playerView) as PlayerView
  internal val volume = itemView.findViewById(R.id.volumeSwitch) as ImageButton
  private val thumbnail = itemView.findViewById(R.id.thumbnail) as ImageView
  private val playAgain = itemView.findViewById(R.id.playerAgain) as Button

  private var video: Video? = null
  private var videoImage: String? = null
  private var videoSources: Sources? = null

  private val videoTag: String?
    get() = this.videoSources?.let { "${javaClass.canonicalName}::${it.file}::$adapterPosition" }

  private val params: Params.() -> Unit
    get() = {
      tag = videoTag
      cover = Glide.with(itemView.context.applicationContext)
          .asBitmap()
          .load(videoImage ?: R.drawable.ic_kohii)
          .submit()
      headlessPlaybackParams = HeadlessPlaybackParams(
          enabled = true, // TODO dynamic condition, eg: network.
          mediaTitle = video?.title ?: "Unknown",
          mediaText = video?.description ?: "Unknown"
      )
    }

  private var binder: Binder<PlayerView>? = null
  private var playback: Playback<*>? = null

  @Suppress("RemoveExplicitTypeArguments")
  private val onBound: (Playback<*>) -> Unit by lazy<(Playback<*>) -> Unit>(NONE) {
    { playback ->
      playback.addPlaybackEventListener(this@VideoViewHolder)
      volume.isSelected = !playback.volumeInfo.mute
      this@VideoViewHolder.playback = playback
    }
  }

  // Trick here: we do not rely on the actual binding to have the Rebinder. This instance will
  // be useful in some verifications.
  internal val rebinder: Rebinder<PlayerView>?
    get() = this.videoTag?.let { Rebinder(it, PlayerView::class.java) }

  override fun setupOnClick(onClick: OnClick) {
    super.setupOnClick(onClick)
    volume.setOnClickListener { onClick.onClick(it, this) }
    playAgain.setOnClickListener {
      if (!playAgain.isVisible) return@setOnClickListener
      // Once completed, a Playback needs to be reset to starting position.
      playback?.rewind()
      binder?.bind(playerView, onBound)
    }
  }

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

      binder = kohii.setUp(videoSources!!.file)
          .with(params)
      /*
      We suppose to do this here, but for a specific scenario of this demo, we need to
      do it when the VideoHolder is attached via adapter#onViewAttachedToWindow.
      */
      // dispatchBindVideo()
    }
  }

  override fun beforePlay(playback: Playback<*>) {
    thumbnail.isVisible = false
    playAgain.isVisible = false
  }

  override fun afterPause(playback: Playback<*>) {
    thumbnail.isVisible = true
  }

  override fun onEnd(playback: Playback<*>) {
    playback.removePlaybackEventListener(this@VideoViewHolder)
    thumbnail.isVisible = true
    playAgain.isVisible = true
  }

  // Called by FbookFragment to immediately reclaim the Rebinder, prevent the Playback to be removed.
  internal fun reclaimRebinder(rebinder: Rebinder<*>) {
    if (shouldBind(rebinder)) {
      binder?.bind(playerView, onBound)
    }
  }

  internal fun dispatchBindVideo() {
    if (shouldBind(this.rebinder)) {
      // bind the Video to PlayerView
      binder?.bind(playerView, onBound)
    }

    playAgain.isVisible = playback?.playbackState == Player.STATE_ENDED
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    video = null
    videoSources = null
    videoImage = null
    playback?.removePlaybackEventListener(this@VideoViewHolder)
    playback = null
    binder = null
  }
}
