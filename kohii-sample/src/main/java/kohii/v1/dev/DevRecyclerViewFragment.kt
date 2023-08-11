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
import android.view.View
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.InitData
import kohii.v1.sample.common.ViewBindingFragment
import kohii.v1.sample.databinding.ActivityDevRecyclerviewBinding

class DevRecyclerViewFragment :
  ViewBindingFragment<ActivityDevRecyclerviewBinding>(ActivityDevRecyclerviewBinding::inflate) {

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    val manager = kohii.register(this)
      .addBucket(requireBinding().recyclerView)

    requireBinding().recyclerView.adapter = DummyAdapter(
      kohii,
      manager,
      enterFullscreenListener = { adapter, holder, _, tag ->
        manager.observe(tag) { _, from, to ->
          if (from?.bucket?.root !== requireBinding().recyclerView && to == null) {
            adapter.bindVideo(holder)
          }
        }

        val intent = PlayerActivity.createIntent(
          requireContext(),
          InitData(tag.toString(), 16 / 9F),
          Rebinder(tag)
        )
        startActivity(intent)
      }
    )
  }
}
