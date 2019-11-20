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

package kohii.v1.sample.ui.pagers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import kohii.v1.core.MemoryMode
import kohii.v1.core.Prioritized
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_debug_child.container

class GridContentFragment : BaseFragment(), Prioritized {

  companion object {
    private const val EXTRA_PAGE_POS = "kohii.v1.demo.pager::page"

    fun newInstance(pagePos: Int = -1) = GridContentFragment().also {
      val args = Bundle()
      args.putInt(EXTRA_PAGE_POS, pagePos)
      it.arguments = args
    }
  }

  private lateinit var kohii: Kohii
  private val pagePos: Int
    get() = arguments?.getInt(EXTRA_PAGE_POS) ?: -1

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_child, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    kohii.register(this, MemoryMode.LOW)
        .attach(container)

    val spanCount = resources.getInteger(R.integer.grid_span)
    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int {
        return if (position % 6 == 3 || position % 6 == spanCount) 2 else 1
      }
    }

    (container.layoutManager as? GridLayoutManager)?.spanSizeLookup = spanSizeLookup
    container.adapter = ItemsAdapter(kohii, pagePos)
  }
}
