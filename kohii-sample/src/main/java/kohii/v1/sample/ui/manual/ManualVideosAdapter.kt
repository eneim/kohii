/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.manual

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Manager
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Controller
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp

internal class ManualVideosAdapter(
  val kohii: Kohii,
  val manager: Manager,
  val enterFullscreenListener: (ManualVideosAdapter, ManualVideoViewHolder, View, Any) -> Unit = { _, _, _, _ -> }
) : Adapter<ManualVideoViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ManualVideoViewHolder {
    val holder = ManualVideoViewHolder(parent)
    holder.enterFullscreen.setOnClickListener {
      enterFullscreenListener(this, holder, holder.playerView, "player::${holder.adapterPosition}")
    }
    return holder
  }

  override fun getItemCount() = Int.MAX_VALUE / 2

  override fun onBindViewHolder(holder: ManualVideoViewHolder, position: Int) {
    bindVideo(holder)
  }

  fun bindVideo(holder: ManualVideoViewHolder) {
    kohii.setUp(DemoApp.assetVideoUri) {
      tag = "player::${holder.adapterPosition}"
      repeatMode = Player.REPEAT_MODE_ONE
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true

        override fun kohiiCanPause(): Boolean = true

        override fun setupRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            renderer.useController = true
            renderer.setControlDispatcher(kohii.createControlDispatcher(playback))
          }
        }
      }
    }
        .bind(holder.playerView)
  }
}
