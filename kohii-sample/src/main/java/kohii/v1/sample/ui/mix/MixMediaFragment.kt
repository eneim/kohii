/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.mix

import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding

/**
 * @author eneim (2018/10/30).
 */
@Keep
class MixMediaFragment : BaseFragment(R.layout.fragment_recycler_view) {

  companion object {
    fun newInstance() = MixMediaFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentRecyclerViewBinding = FragmentRecyclerViewBinding.bind(view)
    val kohii = Kohii[this].also {
      it.register(this)
        .addBucket(binding.recyclerView)
    }

    binding.recyclerView.also {
      it.setHasFixedSize(true)
      it.adapter = ItemsAdapter(getApp().exoItems, kohii)
    }
  }
}
