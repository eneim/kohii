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
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kohii.core.Master
import kohii.core.Playback
import kohii.core.PlayerViewRebinder
import kohii.core.Scope
import kohii.media.VolumeInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.TransitionListenerAdapter
import kohii.v1.sample.common.getApp
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.actionButton
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.bottomSheet
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.recyclerView
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.videoOverlay
import kotlinx.android.synthetic.main.video_overlay_fullscreen.overlayPlayerView
import kotlinx.android.synthetic.main.video_overlay_fullscreen.video_player_container
import kotlin.properties.Delegates

/**
 * @author eneim (2018/07/06).
 */
@Keep
class OverlayViewFragment : BaseFragment(), TransitionListenerAdapter, BackPressConsumer {

  companion object {
    fun newInstance() = OverlayViewFragment()
  }

  private val overlayViewModel: OverlayViewModel by viewModels()

  private var playback: Playback? = null

  private var selection: Pair<Int, PlayerViewRebinder?> by Delegates.observable<Pair<Int, PlayerViewRebinder?>>(
      initialValue = -1 to null,
      onChange = { _, from, to ->
        if (from == to) return@observable
        val (oldPos, oldRebinder) = from
        val (newPos, newRebinder) = to
        if (newRebinder != null) {
          if (overlaySheet.state == STATE_HIDDEN) overlaySheet.state = STATE_EXPANDED
          newRebinder.bind(kohii, overlayPlayerView) {
            kohii.stick(it)
            playback = it
          }
          recyclerView.adapter?.notifyItemChanged(newPos)
        } else {
          if (oldRebinder != null) {
            playback?.also {
              val vh = recyclerView.findViewHolderForAdapterPosition(oldPos)
              if (vh == null) it.unbind() // the VH is out of viewport.
              else recyclerView.adapter?.notifyItemChanged(vh.adapterPosition)
            }
            playback = null
          }
        }
      }
  )

  private lateinit var motionLayout: MotionLayout
  private lateinit var overlaySheet: BottomSheetBehavior<*>
  private lateinit var kohii: Master

  private val sheetCallback = object : BottomSheetCallback() {
    override fun onSlide(
      bottomSheet: View,
      slideOffset: Float
    ) {
      motionLayout.progress = 1 - slideOffset.coerceIn(0F, 1F)
    }

    override fun onStateChanged(
      bottomSheet: View,
      state: Int
    ) {
      if (state == STATE_HIDDEN) {
        deselectRebinder()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view_motion, parent, false)
  }

  internal lateinit var adapter: VideoItemsAdapter

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Master[this]
    val manager = kohii.register(this)
        .attach(recyclerView, video_player_container)

    motionLayout = videoOverlay as MotionLayout

    adapter = VideoItemsAdapter(getApp().videos, kohii,
        onVideoClick = { pos, rebinder -> selectRebinder(pos, rebinder) },
        shouldBindVideo = { /* the Rebinder is not selected */ it != this.selection.second }
    )

    recyclerView.let {
      it.setHasFixedSize(true)
      it.adapter = adapter
      it.layoutManager = LinearLayoutManager(context)
    }

    actionButton.setOnClickListener {
      val current = overlayViewModel.recyclerViewVolume.value!!
      overlayViewModel.recyclerViewVolume.value = VolumeInfo(!current.mute, current.volume)
    }

    overlaySheet = BottomSheetBehavior.from(bottomSheet)
    if (savedInstanceState == null) overlaySheet.state = STATE_HIDDEN
    overlaySheet.addBottomSheetCallback(sheetCallback)

    // Update overlay view's max width on collapse mode/landscape mode.
    val constraintSet = motionLayout.getConstraintSet(R.id.end)
    val collapseWidth = resources.getDimensionPixelSize(R.dimen.overlay_collapse_max_width)
    constraintSet.constrainMaxWidth(R.id.dummy_frame, collapseWidth)
    constraintSet.applyTo(motionLayout)
    motionLayout.setTransitionListener(this)

    overlayViewModel.apply {
      overlayVolume.observe(viewLifecycleOwner) {
        manager.applyVolumeInfo(it, video_player_container, Scope.HOST)
      }

      recyclerViewVolume.observe(viewLifecycleOwner) {
        manager.applyVolumeInfo(it, recyclerView, Scope.HOST)
        actionButton.text = "Mute RV: ${it.mute}"
      }

      selectedRebinder.observe(viewLifecycleOwner) {
        selection = it
      }
    }
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    if (savedInstanceState != null) restoreState()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    overlaySheet.removeBottomSheetCallback(sheetCallback)
    recyclerView.adapter = null
  }

  private fun restoreState() {
    if (overlaySheet.state == STATE_COLLAPSED) {
      motionLayout.progress = 1F
    } else if (overlaySheet.state == STATE_EXPANDED) {
      motionLayout.progress = 0F
    }

    selection.second?.bind(kohii, overlayPlayerView) {
      kohii.stick(it)
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
    return when (overlaySheet.state) {
      STATE_COLLAPSED -> {
        overlaySheet.state = STATE_HIDDEN
        true
      }
      STATE_EXPANDED -> {
        overlaySheet.state = STATE_COLLAPSED
        true
      }
      else -> false
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  internal fun selectRebinder(
    position: Int,
    rebinder: PlayerViewRebinder
  ) {
    overlayViewModel.selectedRebinder.value = position to rebinder
  }

  internal fun deselectRebinder() {
    overlayViewModel.selectedRebinder.value = -1 to null
  }
}
