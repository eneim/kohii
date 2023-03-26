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

package kohii.v1.sample.tiktok.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import kohii.v1.core.MemoryMode.HIGH
import kohii.v1.sample.tiktok.KohiiProvider
import kohii.v1.sample.tiktok.databinding.FragmentHomeBinding
import kohii.v1.sample.tiktok.getApp

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding: FragmentHomeBinding get() = requireNotNull(_binding)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = KohiiProvider[requireContext()]
    kohii.register(this, memoryMode = HIGH, activeLifecycleState = State.RESUMED)
      .addBucket(binding.videos)
    binding.videos.adapter = VideosAdapter(getApp().videos, kohii)
    binding.videos.offscreenPageLimit = 1
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
