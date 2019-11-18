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
import androidx.fragment.app.commit
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView

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
    recyclerView.adapter = DemoItemsAdapter(getApp().demoItems) {
      requireActivity().title =
        if (it.title != 0) getString(it.title) else it.fragmentClass.simpleName
      parentFragmentManager.commit {
        setReorderingAllowed(true) // Optimize for shared element transition
        replace(
            R.id.fragmentContainer, it.fragmentClass.newInstance(),
            it.fragmentClass.canonicalName
        )
        addToBackStack(null)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    requireActivity().title = getString(R.string.app_name)
  }
}
