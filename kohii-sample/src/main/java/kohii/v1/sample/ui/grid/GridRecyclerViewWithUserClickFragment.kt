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

package kohii.v1.sample.ui.grid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.ui.main.DemoItem
import timber.log.Timber

class GridRecyclerViewWithUserClickFragment :
  BaseFragment(),
  DemoContainer,
  GridContentFragment.VideoGridCallback,
  SinglePlayerFragment.Callback {

  companion object {
    fun newInstance() = GridRecyclerViewWithUserClickFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  private val viewModel: VideosViewModel by viewModels()

  private lateinit var videoFragment: GridContentFragment
  private var selected: SelectionKey? = null
    set(value) {
      val from = field
      field = value
      if (from == value /* equals */) return
      if (value != null) {
        videoFragment.select(value)
        val tag = value.rebinder.tag.toString()
        childFragmentManager.findFragmentByTag(tag)
          ?: SinglePlayerFragment.newInstance(value)
            .showNow(childFragmentManager, tag)
      } else {
        if (from != null) videoFragment.deselect(from)
      }
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_grid, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    videoFragment = childFragmentManager.findFragmentById(R.id.mainPanel) as GridContentFragment
    viewModel.selectedRebinder.observe(viewLifecycleOwner) { rebinder ->
      this.selected = rebinder
    }
  }

  // GridContentFragment.Callback

  override fun onSelected(selectionKey: SelectionKey) {
    viewModel.selectedRebinder.value = selectionKey
    Timber.i("Selected: $selectionKey")
  }

  // SinglePlayerFragment.Callback

  override fun onShown(selectionKey: SelectionKey) {
    viewModel.selectedRebinder.value = selectionKey
    Timber.i("Shown: $selectionKey")
  }

  override fun onDismiss(selectionKey: SelectionKey) {
    viewModel.selectedRebinder.value = null
    Timber.i("Dismiss: $selectionKey")
  }
}
