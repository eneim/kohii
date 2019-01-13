/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.transition.TransitionInflater
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlayerEventListener
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_player.playerView

/**
 * To play a single Video.
 *
 * @author eneim (2018/06/26).
 */
class PlayerFragment : BaseFragment() {

  companion object {
    private const val KEY_PLAYABLE_TAG = "kohii:fragment:player:tag"

    fun newInstance(tag: String): PlayerFragment {
      val bundle = Bundle().also {
        it.putString(KEY_PLAYABLE_TAG, tag)
      }
      return PlayerFragment().also { it.arguments = bundle }
    }
  }

  private var listener: PlayerEventListener? = null

  var playback: Playback<*>? = null
  var transView: View? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    listener = object : PlayerEventListener {
      override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
      ) {
        startPostponedEnterTransition()
        playback?.removePlayerEventListener(this)
      }
    }
  }

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

    val playableTag = arguments?.getString(KEY_PLAYABLE_TAG) as String
    transView = playerView.findViewById(R.id.exo_content_frame)
    ViewCompat.setTransitionName(transView!!, playableTag)

    playback = Kohii[requireContext()].findPlayable(playableTag)
        ?.bind(playerView)
        ?.also {
          it.addPlayerEventListener(listener!!)
        }
  }

  override fun onStop() {
    super.onStop()
    playback?.removePlayerEventListener(listener)
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