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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import kohii.v1.exoplayer.Kohii
import kohii.v1.media.VolumeInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding
import kohii.v1.sample.ui.main.DemoItem

// Change VolumeInfo of each Playback individually, and store that info across config change.
class EchoFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = EchoFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  private val viewModel: VolumeStateViewModel by viewModels()
  private lateinit var kohii: Kohii

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
    val binding: FragmentRecyclerViewBinding = FragmentRecyclerViewBinding.bind(view)
    kohii = Kohii[this]
    kohii.register(this)
      .addBucket(binding.recyclerView)

    val adapter = VideoItemsAdapter(getApp().videos, kohii, viewModel) {
      val playback = it.playback
      return@VideoItemsAdapter if (playback != null) {
        val currentVolumeInfo = viewModel.get(it.absoluteAdapterPosition)
        val nextVolumeInfo = VolumeInfo(
          !currentVolumeInfo.mute,
          currentVolumeInfo.volume
        )
        viewModel.set(it.absoluteAdapterPosition, nextVolumeInfo)
        nextVolumeInfo
      } else {
        null
      }
    }

    binding.recyclerView.adapter = adapter
  }
}
