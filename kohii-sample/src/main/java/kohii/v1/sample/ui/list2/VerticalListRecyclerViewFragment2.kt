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

package kohii.v1.sample.ui.list2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.PlayerInfoHolder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding
import kohii.v1.sample.ui.main.DemoItem

/**
 * @author eneim (2018/07/06).
 */
@Keep
class VerticalListRecyclerViewFragment2 : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = VerticalListRecyclerViewFragment2()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  // Should be implemented by Activity, to keep information of latest clicked item position.
  private var playerInfoHolder: PlayerInfoHolder? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    playerInfoHolder = context as? PlayerInfoHolder?
  }

  override fun onDetach() {
    super.onDetach()
    playerInfoHolder = null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, parent, false)
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

    (view.findViewById(R.id.recyclerView) as RecyclerView).also {
      it.setHasFixedSize(true)
      it.layoutManager = LinearLayoutManager(requireContext())
      it.adapter = ItemsAdapter(kohii)
    }
  }
}
