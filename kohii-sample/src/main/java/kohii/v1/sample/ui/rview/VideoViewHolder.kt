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

package kohii.v1.sample.ui.rview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.sample.R
import kohii.v1.sample.ui.rview.data.Item

/**
 * @author eneim (2018/07/06).
 */
class VideoViewHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  private val kohii: Kohii,
  private val containerProvider: ContainerProvider,
  private val listener: OnClickListener
) : BaseViewHolder(inflater, R.layout.holder_player_view, parent),
    View.OnClickListener, PlaybackEventListener, Playback.Callback {

  init {
    itemView.setOnClickListener(this)
  }

  override fun onActive(
    playback: Playback<*>,
    target: Any?
  ) {
    listener.onItemLoaded(itemView, adapterPosition)
  }

  override fun onInActive(
    playback: Playback<*>,
    target: Any?
  ) {
  }

  override fun onFirstFrameRendered() {
    Log.i("KohiiApp:VH:$adapterPosition", "onFirstFrameRendered()")
  }

  override fun onBuffering(playWhenReady: Boolean) {
    Log.i("KohiiApp:VH:$adapterPosition", "onBuffering(): $playWhenReady")
  }

  override fun onPlaying() {
    Log.i("KohiiApp:VH:$adapterPosition", "onPlaying()")
  }

  override fun onPaused() {
    Log.i("KohiiApp:VH:$adapterPosition", "onPaused()")
  }

  override fun onCompleted() {
    Log.i("KohiiApp:VH:$adapterPosition", "onCompleted()")
  }

  val playerView = itemView.findViewById(R.id.playerView) as PlayerView
  val playerContainer = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
  val transView = playerView.findViewById(R.id.exo_content_frame) as View

  var itemTag: String? = null
  var playback: Playback<PlayerView>? = null

  override fun bind(item: Item?) {
    if (item != null) {
      itemTag = "${javaClass.canonicalName}::${item.content}::$adapterPosition"

      playerContainer.setAspectRatio(item.width / item.height.toFloat())
      val playable = kohii
          .setUp(item.content)
          .copy(tag = itemTag, prefetch = true, repeatMode = Player.REPEAT_MODE_ONE)
          .asPlayable()

      playback = playable.bind(containerProvider, playerView)
      playback?.also {
        it.addPlaybackEventListener(this)
        it.addCallback(this)
      } ?: listener.onItemLoadFailed(adapterPosition, RuntimeException("Failed!"))

      ViewCompat.setTransitionName(transView, itemTag)
    }
  }

  override fun onRecycled(success: Boolean) {
    super.onRecycled(success)
    playback?.removePlaybackEventListener(this)
    playback?.removeCallback(this)
    itemView.setOnClickListener(null)
  }

  override fun onClick(v: View?) {
    if (v != null && itemTag != null) {
      listener.onItemClick(v, transView, adapterPosition, itemTag!!)
    }
  }
}