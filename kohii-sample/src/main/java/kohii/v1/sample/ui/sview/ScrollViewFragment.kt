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

package kohii.v1.sample.ui.sview

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.transition.TransitionInflater
import androidx.transition.TransitionSet
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.PlayerEventListener
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.player.PlayerFragment
import kotlinx.android.synthetic.main.fragment_scroll_view.playerContainer
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView

class ScrollViewFragment : BaseFragment(), Playback.Callback, PlayerEventListener, PlaybackEventListener {

  companion object {
    const val videoUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
    fun newInstance() = ScrollViewFragment()
  }

  private var playback: Playback<PlayerView>? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_scroll_view, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    prepareTransitions()
    postponeEnterTransition()

    playback = Kohii[this].setUp(videoUrl)
        .copy(repeatMode = Player.REPEAT_MODE_ONE)
        .copy(tag = videoUrl)
        .copy(config = DemoApp.app.config)
        .asPlayable().bind(playerView).also {
          it.addPlayerEventListener(this@ScrollViewFragment)
          it.addPlaybackEventListener(this@ScrollViewFragment)
          it.addCallback(this@ScrollViewFragment)
        }

    val transView: View = playerView.findViewById(R.id.exo_content_frame)
    ViewCompat.setTransitionName(transView, videoUrl)
  }

  override fun onStart() {
    super.onStart()
    view?.run {
      val transView: View = playerView.findViewById(R.id.exo_content_frame)
      playerContainer.setOnClickListener {
        (exitTransition as TransitionSet).excludeTarget(this, true)
        fragmentManager!!.beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(transView, ViewCompat.getTransitionName(transView)!!)
            .replace(R.id.fragmentContainer, PlayerFragment.newInstance(videoUrl), videoUrl)
            .addToBackStack(null)
            .commit()
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  }

  override fun onStop() {
    super.onStop()
    playback?.removeCallback(this)
    playback?.removePlaybackEventListener(this)
    playback?.removePlayerEventListener(this)
    playerContainer.setOnClickListener(null)
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

  // BEGIN: PlaybackEventListener

  override fun onBuffering(playWhenReady: Boolean) {
  }

  override fun onPlaying() {
    view?.run {
      Snackbar.make(this, "State: Playing", Snackbar.LENGTH_LONG).show()
    }
  }

  override fun onPaused() {
    view?.run {
      Snackbar.make(this, "State: Paused", Snackbar.LENGTH_LONG).show()
    }
  }

  override fun onCompleted() {
    view?.run {
      Snackbar.make(this, "State: Ended", Snackbar.LENGTH_LONG).show()
    }
  }

  // END: PlaybackEventListener

  // BEGIN: Playback.Callback

  override fun onTargetAvailable(playback: Playback<*>) {
    Toast.makeText(requireContext(), "Target available", Toast.LENGTH_SHORT).show()
  }

  override fun onTargetUnAvailable(playback: Playback<*>) {
    Toast.makeText(requireContext(), "Target unavailable", Toast.LENGTH_SHORT).show()
  }

  // END: Playback.Callback

  // BEGIN: PlayerEventListener

  override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float) {
    startPostponedEnterTransition()
    playback?.removePlayerEventListener(this)
  }

  override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
  }

  override fun onSeekProcessed() {
  }

  override fun onTracksChanged(trackGroups: TrackGroupArray?,
      trackSelections: TrackSelectionArray?) {
  }

  override fun onPlayerError(error: ExoPlaybackException?) {
  }

  override fun onLoadingChanged(isLoading: Boolean) {
  }

  override fun onPositionDiscontinuity(reason: Int) {
  }

  override fun onRepeatModeChanged(repeatMode: Int) {
  }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
  }

  override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
  }

  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
  }

  override fun onRenderedFirstFrame() {
  }

  override fun onCues(cues: MutableList<Cue>?) {
  }

  override fun onMetadata(metadata: Metadata?) {
  }

  // END: PlayerEventListener
}
