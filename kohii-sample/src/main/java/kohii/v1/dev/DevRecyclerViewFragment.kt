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

package kohii.v1.dev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Manager
import kohii.v1.exoplayer.DefaultControlDispatcher
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.databinding.ActivityDevRecyclerviewBinding

class DevRecyclerViewFragment : BaseFragment() {

  private var _binding: ActivityDevRecyclerviewBinding? = null
  private val binding: ActivityDevRecyclerviewBinding get() = requireNotNull(_binding)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = ActivityDevRecyclerviewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    val manager = kohii.register(this)
        .addBucket(binding.recyclerView)

    binding.recyclerView.adapter = DummyAdapter(kohii, manager)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

internal class DummyViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_player_view) {
  internal val playerView: PlayerView = itemView.findViewById(R.id.playerView)
}

internal class DummyAdapter(
  val kohii: Kohii,
  val manager: Manager
) : Adapter<DummyViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DummyViewHolder {
    return DummyViewHolder(parent)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE / 2
  }

  override fun onBindViewHolder(
    holder: DummyViewHolder,
    position: Int
  ) {
    kohii.setUp(DemoApp.assetVideoUri) {
      tag = "player::$position"
      if (position == 0) {
        controller = DefaultControlDispatcher(
            manager, holder.playerView,
            kohiiCanStart = false,
            kohiiCanPause = false
        )
      }
    }
        .bind(holder.playerView)
  }
}
