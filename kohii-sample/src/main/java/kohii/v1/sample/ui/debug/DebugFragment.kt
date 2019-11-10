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

package kohii.v1.sample.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import kohii.core.PlayerViewRebinder
import kohii.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.Delegates

@Suppress("unused")
class DebugFragment : BaseFragment(), DebugChildFragment.Callback, SinglePlayerFragment.Callback {

  companion object {
    fun newInstance() = DebugFragment()
  }

  private val viewModel: VideosViewModel by viewModels()
  private val videoFragment by lazy(NONE) {
    childFragmentManager.findFragmentById(R.id.mainPanel) as DebugChildFragment
  }
  private var selected: PlayerViewRebinder? by Delegates.observable<PlayerViewRebinder?>(
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
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.selectedRebinder.observe(viewLifecycleOwner) { rebinder ->
      this.selected = rebinder
    }
  }

  // DebugChildFragment.Callback

  override fun onSelected(rebinder: Rebinder<*>) {
    if (rebinder is PlayerViewRebinder) viewModel.selectedRebinder.value = rebinder
  }

  // SinglePlayerFragment.Callback

  override fun onShown(rebinder: PlayerViewRebinder) {
    viewModel.selectedRebinder.value = rebinder
  }

  override fun onDismiss(rebinder: PlayerViewRebinder) {
    viewModel.selectedRebinder.value = null
  }
}
