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

package kohii.v1.sample.ui.nested

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kohii.v1.Kohii
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.fragment_scrollview_recyclerview.scrollView
import kotlinx.android.synthetic.main.holder_nested_recyclereview.recyclerView

// Nested RecyclerView in NestedScrollView
class RvInSvFragment : BaseNestedFragment() {

  companion object {
    fun newInstance() = RvInSvFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_scrollview_recyclerview, container, false)
  }

  private val snapHelper = PagerSnapHelper()

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    kohii.register(this, arrayOf(scrollView, recyclerView))

    (recyclerView as RecyclerView).also {
      it.setHasFixedSize(true)
      it.adapter = ItemsAdapter(videos, kohii)
    }
    ViewCompat.setNestedScrollingEnabled(recyclerView, false)
    snapHelper.attachToRecyclerView(recyclerView)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    snapHelper.attachToRecyclerView(null)
  }
}
