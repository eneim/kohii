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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kohii.v1.ads.Manilo
import kohii.v1.core.MemoryMode.INFINITE
import kohii.v1.sample.common.ViewBindingFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source

class AdListFragment :
    ViewBindingFragment<FragmentRecyclerViewBinding>(FragmentRecyclerViewBinding::inflate) {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val manilo = Manilo[this]
    manilo.register(this, memoryMode = INFINITE).addBucket(requireBinding().recyclerView)

    val app = getApp()
    viewLifecycleOwner.lifecycleScope.launchWhenCreated {
      val items = withContext(Dispatchers.IO) {
        app.moshi.adapter(AdSamples::class.java)
            .fromJson(app.assets.open("ads.json").source().buffer())
            ?: AdSamples("No Ads", emptyList())
      }
      val adapter = ItemsAdapter(manilo, items.samples)
      requireBinding().recyclerView.adapter = adapter
    }
  }
}
