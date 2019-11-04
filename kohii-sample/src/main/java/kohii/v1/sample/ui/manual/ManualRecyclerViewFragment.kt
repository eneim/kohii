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

package kohii.v1.sample.ui.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlaybackManager
import kohii.v1.exo.DefaultControlDispatcher
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.BaseViewHolder
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView
import kotlinx.android.synthetic.main.holder_player_view.view.playerView

const val videoUrl = "https://content.jwplatform.com/manifests/146UwF4L.m3u8"

class ManualRecyclerViewFragment : BaseFragment() {

  companion object {
    fun newInstance() = ManualRecyclerViewFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    val manager = kohii.register(this, recyclerView)

    recyclerView.adapter = Adapter(kohii, manager)
  }
}

// Implementations

class TextViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, android.R.layout.simple_list_item_1) {

  override fun bind(item: Any?) {
    super.bind(item)
    (itemView as TextView).text = itemView.context.getString(R.string.lib_intro)
        .parseAsHtml()
  }
}

class VideoViewHolder(
  parent: ViewGroup,
  val kohii: Kohii,
  val manager: PlaybackManager
) : BaseViewHolder(parent, R.layout.holder_player_view) {

  val playerView: PlayerView = itemView.playerView

  override fun bind(item: Any?) {
    super.bind(item)
    kohii.setUp(videoUrl)
        .with {
          tag = videoUrl
          repeatMode = Playable.REPEAT_MODE_ONE
          controller = DefaultControlDispatcher(
              manager, playerView,
              kohiiCanStart = false,
              kohiiCanPause = true
          )
        }
        .bind(playerView)
  }
}

class Adapter(
  val kohii: Kohii,
  val manager: PlaybackManager
) : Adapter<BaseViewHolder>() {

  companion object {
    const val TYPE_VIDEO = 1
    const val TYPE_TEXT = 2
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return if (viewType == TYPE_TEXT) TextViewHolder(parent) else
      VideoViewHolder(parent, kohii, manager)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(position)
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 2) TYPE_VIDEO else TYPE_TEXT
  }
}
