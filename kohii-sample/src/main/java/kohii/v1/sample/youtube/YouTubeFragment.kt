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

package kohii.v1.sample.youtube

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import kohii.v1.Kohii
import kohii.v1.LifecycleOwnerProvider
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView

class YouTubeFragment : BaseFragment(), LifecycleOwnerProvider {

  companion object {
    fun newInstance() = YouTubeFragment()
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
    Kohii[this].also { it.register(this, arrayOf(recyclerView)) }
    val creator = (requireActivity().application as DemoApp).creator
    recyclerView.adapter = YouTubeItemsAdapter(creator)
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}
