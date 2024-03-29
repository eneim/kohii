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

package kohii.v1.sample.ui.youtube

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kohii.v1.experiments.UnofficialYouTubePlayerEngine
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding
import kohii.v1.sample.ui.main.DemoItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class YouTubeFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = YouTubeFragment()
  }

  private val viewModel: YouTubeViewModel by viewModels()
  private lateinit var binding: FragmentRecyclerViewBinding

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding: FragmentRecyclerViewBinding =
      FragmentRecyclerViewBinding.inflate(inflater, container, false)
    this.binding = binding
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val engine = UnofficialYouTubePlayerEngine[requireContext()]
    engine.register(this)
      .addBucket(binding.recyclerView)

    val adapter = YouTubePlaylistPagingAdapter(engine)
    binding.recyclerView.adapter = adapter

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(State.STARTED) {
        viewModel.pagingData.collectLatest(adapter::submitData)
      }
    }

    viewModel.load(YouTubeViewModel.YOUTUBE_PLAYLIST_ID)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.recyclerView.adapter = null
  }
}
