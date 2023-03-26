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

import android.view.ViewGroup
import androidx.core.view.isVisible
import kohii.v1.core.Playback
import kohii.v1.sample.common.ViewBindingHolder
import kohii.v1.sample.databinding.ManualVideoHolderBinding

internal class ManualVideoViewHolder(
  parent: ViewGroup
) : ViewBindingHolder<ManualVideoHolderBinding>(parent, ManualVideoHolderBinding::inflate),
  Playback.StateListener {

  init {
    binding.controller.exoPause.isVisible = false
    binding.controller.exoPlay.isVisible = true
  }

  override fun onPlaying(playback: Playback) {
    binding.controller.exoPause.isVisible = true
    binding.controller.exoPlay.isVisible = false
  }

  override fun onPaused(playback: Playback) {
    binding.controller.exoPause.isVisible = false
    binding.controller.exoPlay.isVisible = true
  }
}
