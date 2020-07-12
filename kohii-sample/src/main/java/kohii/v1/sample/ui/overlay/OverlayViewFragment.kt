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
import kohii.v1.core.Manager
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.core.Scope
import kohii.v1.exoplayer.Kohii
import kohii.v1.media.VolumeInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.TransitionListenerAdapter
import kohii.v1.sample.common.getApp
import kohii.v1.sample.databinding.FragmentRecyclerViewMotionBinding
import kohii.v1.sample.ui.main.DemoItem
import kohii.v1.viewBehavior
import kotlin.properties.Delegates

/**
 * @author eneim (2018/07/06).
 */
@Suppress("MemberVisibilityCanBePrivate")
@Keep
class OverlayViewFragment : BaseFragment(), TransitionListenerAdapter, BackPressConsumer,
    DemoContainer {

  companion object {
    fun newInstance() = OverlayViewFragment()
  }

  override val demoItem: DemoItem? get() = arguments?.getParcelable(KEY_DEMO_ITEM)

  private val overlayViewModel: OverlayViewModel by viewModels()

  private val sheetCallback = object : BottomSheetCallback() {
    override fun onSlide(
      bottomSheet: View,
      slideOffset: Float
    ) {
      binding.motionLayout.progress = 1F - slideOffset.coerceIn(0F, 1F)
    }

    override fun onStateChanged(
      bottomSheet: View,
      state: Int
    ) {
      if (state == STATE_HIDDEN) deselectRebinder()
    }
  }

  private var playback: Playback? = null
  private var selection by Delegates.observable<Pair<Int, Rebinder?>>(
      initialValue = -1 to null,
      onChange = { _, from, to ->
        if (from == to) return@observable
        val (oldPos, oldRebinder) = from
        val (newPos, newRebinder) = to
        if (newRebinder != null) {
          if (overlaySheet.state == STATE_HIDDEN) overlaySheet.state = STATE_EXPANDED
          newRebinder.bind(kohii, binding.overlayPlayerView) {
            kohii.stick(it)
            playback = it
          }
          binding.recyclerView.adapter?.notifyItemChanged(newPos)
        } else {
          if (oldRebinder != null) {
            playback?.also {
              val vh = binding.recyclerView.findViewHolderForAdapterPosition(oldPos)
              if (vh == null) it.unbind() // the VH is out of viewport.
              binding.recyclerView.adapter?.notifyItemChanged(oldPos)
            }
            playback = null
          }
        }
      }
  )

  private lateinit var kohii: Kohii
  private lateinit var manager: Manager
  private lateinit var binding: FragmentRecyclerViewMotionBinding
  private lateinit var overlaySheet: BottomSheetBehavior<*>
  private lateinit var adapter: VideoItemsAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    binding = FragmentRecyclerViewMotionBinding.inflate(inflater, parent, false)
    return binding.root
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    manager = kohii.register(this)
        .addBucket(binding.recyclerView)
        .addBucket(binding.videoPlayerContainer)

    adapter = VideoItemsAdapter(getApp().videos, kohii,
        shouldBindVideo = { /* the Rebinder is not selected */ it != selection.second },
        onVideoClick = { pos, rebinder -> selectRebinder(pos, rebinder) }
    )

    binding.recyclerView.let {
      it.setHasFixedSize(true)
      it.adapter = adapter
      it.layoutManager = LinearLayoutManager(context)
    }

    binding.actionButton.setOnClickListener {
      val current = overlayViewModel.recyclerViewVolume.value!!
      overlayViewModel.recyclerViewVolume.value =
        VolumeInfo(!current.mute, current.volume)
    }

    // We will fetch the behavior manually ...
    // overlaySheet = BottomSheetBehavior.from(binding.motionLayout) // don't do this :(
    overlaySheet = run {
      val behavior = binding.motionLayout.viewBehavior()
      check(behavior is BottomSheetBehavior)
      behavior
    }

    if (savedInstanceState == null) overlaySheet.state = STATE_HIDDEN
    overlaySheet.addBottomSheetCallback(sheetCallback)

    // Update overlay view's max width on collapse mode/landscape mode.
    val constraintSet = binding.motionLayout.getConstraintSet(R.id.end)
    val collapseWidth = resources.getDimensionPixelSize(R.dimen.overlay_collapse_max_width)
    constraintSet.constrainMaxWidth(R.id.dummy_frame, collapseWidth)
    constraintSet.applyTo(binding.motionLayout)
    binding.motionLayout.setTransitionListener(this)

    overlayViewModel.apply {
      overlayVolume.observe(viewLifecycleOwner) {
        manager.applyVolumeInfo(it, binding.videoPlayerContainer, Scope.BUCKET)
      }

      recyclerViewVolume.observe(viewLifecycleOwner) {
        manager.applyVolumeInfo(it, binding.recyclerView, Scope.BUCKET)
        binding.actionButton.text = "Mute RV: ${it.mute}"
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
    binding.recyclerView.adapter = null
  }

  private fun restoreState() {
    if (overlaySheet.state == STATE_COLLAPSED) {
      binding.motionLayout.progress = 1F
    } else if (overlaySheet.state == STATE_EXPANDED) {
      binding.motionLayout.progress = 0F
    }

    selection.second?.bind(kohii, binding.overlayPlayerView) {
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
    binding.overlayPlayerView.useController = progress < 0.2
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

  internal fun selectRebinder(
    position: Int,
    rebinder: Rebinder
  ) {
    overlayViewModel.selectedRebinder.value = position to rebinder
  }

  internal fun deselectRebinder() {
    overlayViewModel.selectedRebinder.value = -1 to null
  }
}
