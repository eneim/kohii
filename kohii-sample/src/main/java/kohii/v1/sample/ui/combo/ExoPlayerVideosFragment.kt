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

package kohii.v1.sample.ui.combo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.commit
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.InitData
import kohii.v1.sample.common.getApp
import kohii.v1.sample.ui.main.DemoItem
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView

/**
 * Sample that uses Videos from ExoPlayer videos.
 * Clicking to an item will open fullscreen landscape Player.
 */
class ExoPlayerVideosFragment : BaseFragment(), DemoContainer {

  companion object {
    fun newInstance() = ExoPlayerVideosFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

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
    postponeEnterTransition()
    recyclerView.doOnNextLayout { startPostponedEnterTransition() }

    val kohii = Kohii[this]
    kohii.register(this)
        .addBucket(recyclerView)

    recyclerView.adapter = ExoVideosAdapter(kohii, getApp().exoItems,
        onClick = { holder, _ ->
          holder.rebinder?.let {
            val player = LandscapeFullscreenFragment.newInstance(
                it,
                InitData(it.tag.toString(), holder.aspectRatio)
            )

            parentFragmentManager.commit {
              setReorderingAllowed(true) // required for Activity-like lifecycle changing.
              replace(R.id.fragmentContainer, player, it.tag.toString())
              addToBackStack(null)
            }
          }
        }
    )
  }
}
