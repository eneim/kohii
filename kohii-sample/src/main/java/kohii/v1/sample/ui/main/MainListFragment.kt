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

package kohii.v1.sample.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewBinding

class MainListFragment : BaseFragment() {

  companion object {
    fun newInstance() = MainListFragment()
  }

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
    binding.recyclerView.adapter = DemoItemsAdapter(getApp().demoItems) {
      val fragment: Fragment = it.fragmentClass.newInstance().apply {
        val bundle = Bundle()
        bundle.putParcelable(KEY_DEMO_ITEM, it)
        arguments = bundle
      }

      parentFragmentManager.commit {
        setReorderingAllowed(true) // Optimize for shared element transition
        replace(
          R.id.fragmentContainer,
          fragment,
          it.fragmentClass.canonicalName
        )
        addToBackStack(null)
      }
    }
  }
}
