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
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.support.v4.app.SharedElementCallback
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kohii.v1.DefaultEventListener
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.player_fragment.playerView

/**
 * To play a single Video.
 *
 * @author eneim (2018/06/26).
 */
class PlayerFragment : Fragment() {

  companion object {
    private const val KEY_PLAYABLE_TAG = "kohii:fragment:player:tag"

    fun newInstance(tag: String): PlayerFragment {
      val bundle = Bundle().also {
        it.putString(KEY_PLAYABLE_TAG, tag)
      }
      return PlayerFragment().also { it.arguments = bundle }
    }
  }

  private val listener: PlayerEventListener by lazy {
    object : DefaultEventListener() {
      override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
          pixelWidthHeightRatio: Float) {
        startPostponedEnterTransition()
        playable?.removePlayerEventListener(this)
      }
    }
  }

  var playable: Playable? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.player_fragment, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
    if (savedInstanceState == null) {
      postponeEnterTransition()
    }

    val playableTag = arguments?.getString(KEY_PLAYABLE_TAG) as String
    playable = Kohii[requireContext()].findPlayable(playableTag)!!

    ViewCompat.setTransitionName(playerView.findViewById(R.id.exo_content_frame), playableTag)

    playable!!.addPlayerEventListener(listener)
    prepareSharedElementTransition()
    playable!!.bind(playerView)
  }

  override fun onStop() {
    super.onStop()
    playable?.removePlayerEventListener(listener)
  }

  /**
   * Prepares the shared element transition from and back to the grid fragment.
   */
  private fun prepareSharedElementTransition() {
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_shared_element_transition)
    transition.duration = 375
    sharedElementEnterTransition = transition

    // A similar mapping is set at the GridFragment with a setExitSharedElementCallback.
    setEnterSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(names: MutableList<String>?,
          sharedElements: MutableMap<String, View>?) {
        // Map the first shared element name to the child ImageView.
        if (view !== null) {
          sharedElements?.put(names?.get(0)!!, playerView.findViewById(R.id.exo_content_frame))
        }
      }
    })
  }
}