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

package kohii.v1.sample.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.transition.TransitionInflater
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.v1.core.Prioritized
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.InitData
import kotlinx.android.synthetic.main.fragment_player.playerContainer
import kotlinx.android.synthetic.main.fragment_player.playerView

/**
 * To play a single Video.
 *
 * @author eneim (2018/06/26).
 */
open class PlayerFragment : BaseFragment(), Prioritized {

  companion object {
    private const val KEY_INIT_DATA = "kohii:fragment:player:init_data"
    private const val KEY_REBINDER = "kohii:fragment:player:rebinder"

    fun newInstance(
      rebinder: Rebinder,
      initData: InitData
    ): PlayerFragment {
      val bundle = Bundle().also {
        it.putParcelable(KEY_REBINDER, rebinder)
        it.putParcelable(KEY_INIT_DATA, initData)
      }
      return PlayerFragment()
          .also { it.arguments = bundle }
    }
  }

  var transView: View? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_player, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
    if (savedInstanceState == null) {
      postponeEnterTransition()
    }
    prepareSharedElementTransition()

    val (initData, rebinder) = requireArguments().let {
      requireNotNull(it.getParcelable<InitData>(KEY_INIT_DATA)) to
          requireNotNull(it.getParcelable<Rebinder>(KEY_REBINDER))
    }

    val container = playerView.findViewById(R.id.exo_content_frame) as AspectRatioFrameLayout
    container.setAspectRatio(initData.aspectRatio)

    ViewCompat.setTransitionName(container, initData.tag)
    transView = container

    val kohii = Kohii[this].also {
      it.register(this)
          .attach(playerContainer)
    }

    rebinder.bind(kohii, playerView) {
      startPostponedEnterTransition()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    transView?.also { ViewCompat.setTransitionName(it, null) }
  }

  /**
   * Prepares the shared element transition from and back to the grid fragment.
   */
  private fun prepareSharedElementTransition() {
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_shared_element_transition)
    transition.duration = 275
    sharedElementEnterTransition = transition

    // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
    setEnterSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
      ) {
        // Map the first shared element name to the child ImageView.
        if (view !== null && transView != null) {
          sharedElements?.put(names?.get(0)!!, transView!!)
        }
      }
    })
  }
}
