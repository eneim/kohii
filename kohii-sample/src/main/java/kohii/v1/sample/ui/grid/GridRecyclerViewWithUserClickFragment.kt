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
import androidx.lifecycle.observe
import kohii.v1.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.ui.main.DemoItem
import timber.log.Timber
import kotlin.properties.Delegates

class GridRecyclerViewWithUserClickFragment : BaseFragment(), DemoContainer,
    GridContentFragment.VideoGridCallback,
    SinglePlayerFragment.Callback {

  companion object {
    fun newInstance() = GridRecyclerViewWithUserClickFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  private val viewModel: VideosViewModel by viewModels()

  private lateinit var videoFragment: GridContentFragment
  private var selected: Rebinder? by Delegates.observable<Rebinder?>(
      null,
      onChange = { _, from, to ->
        if (from == to /* equals */) return@observable
        if (to != null) {
          videoFragment.select(to)
          val tag = to.tag.toString()
          childFragmentManager.findFragmentByTag(tag)
              ?: SinglePlayerFragment.newInstance(to)
                  .show(childFragmentManager, tag)
        } else {
          if (from != null) videoFragment.deselect(from)
        }
      })

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

  override fun onSelected(rebinder: Rebinder) {
    viewModel.selectedRebinder.value = rebinder
    Timber.i("Selected: $rebinder")
  }

  // SinglePlayerFragment.Callback

  override fun onShown(rebinder: Rebinder) {
    viewModel.selectedRebinder.value = rebinder
    Timber.i("Shown: $rebinder")
  }

  override fun onDismiss(rebinder: Rebinder) {
    viewModel.selectedRebinder.value = null
    Timber.i("Dismiss: $rebinder")
  }
}
