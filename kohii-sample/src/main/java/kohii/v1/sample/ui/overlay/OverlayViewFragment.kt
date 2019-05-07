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

package kohii.v1.sample.ui.overlay

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.Rebinder
import kohii.v1.Scope
import kohii.v1.TargetHost
import kohii.v1.sample.DemoApp
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.TransitionListenerAdapter
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.actionButton
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.bottomSheet
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.recyclerView
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.videoOverlay
import kotlinx.android.synthetic.main.video_overlay_fullscreen.overlayPlayerView
import kotlinx.android.synthetic.main.video_overlay_fullscreen.video_player_container

/**
 * @author eneim (2018/07/06).
 */
@Keep
class OverlayViewFragment : BaseFragment(), TransitionListenerAdapter, BackPressConsumer {

  companion object {
    fun newInstance() = OverlayViewFragment()
  }

  private var overlaySheet: BottomSheetBehavior<*>? = null
  private var rebinder: Rebinder? = null
  private var playback: Playback<*>? = null

  private lateinit var selectionTracker: SelectionTracker<Rebinder>
  private lateinit var keyProvider: VideoTagKeyProvider

  private lateinit var kohii: Kohii
  private lateinit var rvHost: TargetHost
  private lateinit var overlayHost: TargetHost
  private val volumeViewModel: VolumeViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view_motion, parent, false)
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this].also {
      val manager = it.register(this)
      overlayHost = manager.registerTargetHost(TargetHost.Builder(video_player_container))!!
      rvHost = manager.registerTargetHost(TargetHost.Builder(recyclerView))!!
    }

    val videoAdapter = VideoItemsAdapter((requireActivity().application as DemoApp).videos, kohii)
    recyclerView.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = videoAdapter
    }

    volumeViewModel.overlayVolume.observe({ viewLifecycleOwner.lifecycle }) {
      kohii.applyVolumeInfo(it, overlayHost, Scope.HOST)
    }

    volumeViewModel.recyclerViewVolume.observe({ viewLifecycleOwner.lifecycle }) {
      kohii.applyVolumeInfo(it, rvHost, Scope.HOST)
      actionButton.text = "Mute RV: ${it.mute}"
    }

    actionButton.setOnClickListener {
      val current = volumeViewModel.recyclerViewVolume.value!!
      volumeViewModel.recyclerViewVolume.value = VolumeInfo(!current.mute, current.volume)
    }

    // Selection
    keyProvider = VideoTagKeyProvider(recyclerView)
    selectionTracker = SelectionTracker.Builder<Rebinder>(
        "caminandes.json",
        recyclerView,
        keyProvider,
        VideoItemLookup(recyclerView),
        StorageStrategy.createParcelableStorage(Rebinder::class.java)
    )
        .withSelectionPredicate(SelectionPredicates.createSelectSingleAnything())
        .build()
        .also {
          videoAdapter.selectionTracker = it
          it.onRestoreInstanceState(savedInstanceState)
        }

    val sheet = BottomSheetBehavior.from(bottomSheet)
    overlaySheet = sheet

    if (savedInstanceState == null) sheet.state = STATE_HIDDEN
    sheet.setBottomSheetCallback(object : BottomSheetCallback() {
      override fun onSlide(
        bottomSheet: View,
        slideOffset: Float
      ) {
        (this@OverlayViewFragment.videoOverlay as MotionLayout).progress =
          1 - slideOffset.coerceIn(0F, 1F)
      }

      override fun onStateChanged(
        bottomSheet: View,
        state: Int
      ) {
        if (state == STATE_HIDDEN) {
          selectionTracker.clearSelection()
          rebinder?.also {
            val pos = keyProvider.getPosition(it)
            val vh = recyclerView.findViewHolderForAdapterPosition(pos)
            if (vh == null) playback?.unbind()
          }
          rebinder = null
          playback = null
        }
      }
    })

    selectionTracker.addObserver(object : SelectionObserver<Rebinder>() {
      override fun onItemStateChanged(
        key: Rebinder,
        selected: Boolean
      ) {
        if (selected && key !== rebinder) {
          rebinder = key
          key.rebind(
              kohii, overlayPlayerView, Playback.Config(priority = Playback.PRIORITY_HIGH)
          ) {
            playback = it
            sheet.state = STATE_EXPANDED
          }
        }
      }
    })

    // Update overlay view's max width on collapse mode/landscape mode.
    (this.videoOverlay as MotionLayout).also {
      val constraintSet = it.getConstraintSet(R.id.end)
      constraintSet.constrainMaxWidth(
          R.id.dummy_frame, resources.getDimensionPixelSize(R.dimen.overlay_collapse_max_width)
      )
      constraintSet.applyTo(it)
      it.setTransitionListener(this)
    }
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    if (savedInstanceState != null) restoreState()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    selectionTracker.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerView.adapter = null
  }

  private fun restoreState() {
    if (overlaySheet?.state == STATE_COLLAPSED) {
      (videoOverlay as? MotionLayout)?.progress = 1F
    } else if (overlaySheet?.state == STATE_EXPANDED) {
      (videoOverlay as? MotionLayout)?.progress = 0F
    }

    rebinder = selectionTracker.selection?.firstOrNull()
    rebinder?.rebind(
        kohii, overlayPlayerView, Playback.Config(priority = Playback.PRIORITY_HIGH)
    ) {
      playback = it
    }
  }

  // MotionLayout.TransitionListener

  override fun onTransitionChange(
    motionLayout: MotionLayout,
    startId: Int,
    endId: Int,
    progress: Float
  ) {
    overlayPlayerView.useController = progress < 0.2
  }

  override fun consumeBackPress(): Boolean {
    return overlaySheet?.let {
      return when {
        it.state == STATE_COLLAPSED -> {
          it.state = STATE_HIDDEN
          true
        }
        it.state == STATE_EXPANDED -> {
          it.state = STATE_COLLAPSED
          true
        }
        else -> false
      }
    } ?: false
  }
}
