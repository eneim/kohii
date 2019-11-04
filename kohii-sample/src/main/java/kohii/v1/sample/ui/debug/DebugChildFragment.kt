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

package kohii.v1.sample.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kohii.dev.Master
import kohii.dev.Playable.Config
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.inflateView
import kohii.v1.sample.ui.manual.videoUrl

class ItemViewHolder(itemView: View) : ViewHolder(itemView) {

  val content = itemView.findViewById(R.id.playerContainer) as ViewGroup
}

class ItemsAdapter(
  private val master: Master,
  private val host: Any
) : Adapter<ItemViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ItemViewHolder {
    val itemView = parent.inflateView(R.layout.holder_player_view)
    return ItemViewHolder(itemView)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: ItemViewHolder,
    position: Int
  ) {
    master.setUp(videoUrl)
        .bind(holder.content, Config(tag = "HOLDER::${holder.adapterPosition}"))
  }
}

class DebugChildFragment : BaseFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_child, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val master = Master[this]
    val container = view.findViewById(R.id.container) as RecyclerView
    master.register(this, container)
    container.adapter = ItemsAdapter(master, this)
  }
}
