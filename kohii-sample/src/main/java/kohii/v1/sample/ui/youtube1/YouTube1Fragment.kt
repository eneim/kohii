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

package kohii.v1.sample.ui.youtube1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import kohii.v1.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView

class YouTube1Fragment : BaseFragment() {

  companion object {
    fun newInstance() = YouTube1Fragment()
  }

  private val viewModel: YouTubeViewModel by viewModels()

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
    val kh = Kohii[this].also { it.register(this, recyclerView) }
    // val creator = (requireActivity().application as DemoApp).youTubePlayableCreator
    val creator = kohii.v1.ytb.YouTubePlayableCreator(kh)
    // val creator = YouTubePlayableCreator(kohii)
    val adapter = YouTubeItemsAdapter(creator, childFragmentManager)
    recyclerView.adapter = adapter

    viewModel.posts.observe(viewLifecycleOwner) {
      adapter.submitList(it)
    }

    viewModel.networkState.observe(viewLifecycleOwner) {
      adapter.setNetworkState(it)
    }

    viewModel.refreshState.observe(viewLifecycleOwner) {
      adapter.setNetworkState(it)
    }

    viewModel.loadPlaylist(YouTubeViewModel.YOUTUBE_PLAYLIST_ID)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerView.adapter = null
  }
}
