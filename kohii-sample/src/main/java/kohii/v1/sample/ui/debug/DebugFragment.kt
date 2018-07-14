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

package kohii.v1.sample.ui.debug

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.Player
import kohii.v1.DefaultEventListener
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.ui.player.PlayerFragment
import kotlinx.android.synthetic.main.fragment_debug.bindSameView
import kotlinx.android.synthetic.main.fragment_debug.playerContainer
import kotlinx.android.synthetic.main.fragment_debug.playerView
import kotlinx.android.synthetic.main.fragment_debug.playerView2
import kotlinx.android.synthetic.main.fragment_debug.switchView
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author eneim (2018/07/13).
 */
class DebugFragment : Fragment() {

  companion object {
    fun newInstance() = DebugFragment()
    const val videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
  }

  private val listener: PlayerEventListener by lazy {
    object : DefaultEventListener() {
      override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
          pixelWidthHeightRatio: Float) {
        startPostponedEnterTransition()
        playable.removePlayerEventListener(this)
      }
    }
  }

  private val playable: Playable by lazy {
    Kohii[this].setUp(Uri.parse(videoUrl))
        .copy(repeatMode = Player.REPEAT_MODE_ONE)
        .copy(tag = videoUrl)
        .copy(config = DemoApp.app.config)
        .asPlayable()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    prepareTransitions()
    postponeEnterTransition()

    playable.addPlayerEventListener(listener)
    val transView: View = playerView.findViewById(R.id.exo_content_frame)
    ViewCompat.setTransitionName(transView, videoUrl)

    playerContainer.setOnClickListener {
      (exitTransition as TransitionSet).excludeTarget(view, true)
      fragmentManager!!.beginTransaction()
          .setReorderingAllowed(true)
          .addSharedElement(transView, ViewCompat.getTransitionName(transView)!!)
          .replace(R.id.fragmentContainer, PlayerFragment.newInstance(videoUrl), videoUrl)
          .addToBackStack(null)
          .commit()
    }

    playable.bind(playerView)

    val views = arrayOf(playerView, playerView2)
    val current = AtomicInteger(0)

    // Debug some certain functions.
    switchView.setOnClickListener { playable.bind(views[current.incrementAndGet() % views.size]) }
    bindSameView.setOnClickListener { playable.bind(views[current.get() % views.size]) }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  }

  override fun onStop() {
    super.onStop()
    playable.removePlayerEventListener(listener)
  }

  private fun prepareTransitions() {
    // Hmm Google https://stackoverflow.com/questions/49461738/transitionset-arraylist-size-on-a-null-object-reference
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_exit_transition)
    transition.duration = 375
    exitTransition = transition

    // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
    setExitSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(names: MutableList<String>?,
          sharedElements: MutableMap<String, View>?) {
        // Map the first shared element name to the child ImageView.
        sharedElements?.put(names?.get(0)!!, playerView.findViewById(R.id.exo_content_frame))
      }
    })
  }
}