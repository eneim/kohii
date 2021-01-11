/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.ads

import android.view.ViewGroup
import kohii.v1.ads.AdMediaItem
import kohii.v1.ads.Manilo
import kohii.v1.sample.common.ViewBindingHolder
import kohii.v1.sample.databinding.HolderPlayerViewBinding

class ItemViewHolder(
  private val manilo: Manilo,
  parent: ViewGroup
) : ViewBindingHolder<HolderPlayerViewBinding>(
    parent, HolderPlayerViewBinding::inflate
) {

  override fun bind(item: Any?) {
    val video: AdSample = item as AdSample? ?: return
    val adMedia = AdMediaItem(
        video.contentUri,
        video.adTagUri
    )

    val binder = manilo.setUp(adMedia) {
      preload = true
      tag = "${video.contentUri}::${adapterPosition}"
    }

    binder.bind(binding.playerContainer)
  }
}
