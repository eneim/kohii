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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.TransitionListenerAdapter
import kohii.v1.sample.ui.overlay.data.Video
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.bottomSheet
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.recyclerView
import kotlinx.android.synthetic.main.fragment_recycler_view_motion.videoOverlay
import kotlinx.android.synthetic.main.video_overlay_fullscreen.overlayPlayerView
import kotlinx.android.synthetic.main.video_overlay_fullscreen.video_player_container
import okio.buffer
import okio.source

/**
 * @author eneim (2018/07/06).
 */
@Suppress("unused")
@Keep
class OverlayViewFragment : BaseFragment(),
    TransitionListenerAdapter, BackPressConsumer, ContainerProvider {

  companion object {
    fun newInstance() = OverlayViewFragment()
    const val TAG = "Kohii@Overlay"
  }

  // TODO use dependency injection in production
  val kohii: Kohii by lazy { Kohii[requireContext()] }

  private val videos by lazy {
    val asset = requireActivity().application.assets
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val jsonAdapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    jsonAdapter.fromJson(asset.open("caminandes.json").source().buffer()) ?: emptyList()
  }

  private var overlaySheet: BottomSheetBehavior<*>? = null
  private var selectionTracker: SelectionTracker<String>? = null
  private var playback: Playback<*>? = null

  private var viewModel: SelectionViewModel? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view_motion, parent, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    viewModel = ViewModelProviders.of(this)
        .get(SelectionViewModel::class.java)
        .apply {
          liveData.observe(viewLifecycleOwner, Observer {
            if (it.second) { // selected
            }
          })
        }

    // Update overlay view's max width on collapse mode.
    val constraintSet = (this.videoOverlay as MotionLayout).getConstraintSet(R.id.end)
    constraintSet.constrainMaxWidth(
        R.id.dummy_frame, resources.getDimensionPixelSize(R.dimen.overlay_collapse_max_width)
    )
    constraintSet.applyTo(this.videoOverlay as MotionLayout)

    val videoAdapter = VideoItemsAdapter(videos, kohii, this)
    val keyProvider = VideoTagKeyProvider(recyclerView)

    recyclerView.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = videoAdapter
    }

    // Selection
    selectionTracker = SelectionTracker.Builder<String>(
        "caminandes.json",
        recyclerView,
        keyProvider,
        VideoItemLookup(recyclerView),
        StorageStrategy.createStringStorage()
    )
        .withSelectionPredicate(SelectionPredicates.createSelectSingleAnything())
        .build()
        .also {
          it.onRestoreInstanceState(savedInstanceState)
          videoAdapter.selectionTracker = it
        }

    overlaySheet = BottomSheetBehavior.from(bottomSheet)
        .also { sheet ->
          if (savedInstanceState == null) sheet.state = STATE_HIDDEN
          sheet.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onSlide(
              bottomSheet: View,
              slideOffset: Float
            ) {
              (videoOverlay as MotionLayout).progress = 1 - slideOffset.coerceIn(0F, 1F)
            }

            override fun onStateChanged(
              bottomSheet: View,
              state: Int
            ) {
              if (state == STATE_HIDDEN) {
                selectionTracker?.clearSelection()
                (playback?.tag as? String)?.let {
                  val pos = keyProvider.getPosition(it)
                  val vh = recyclerView.findViewHolderForAdapterPosition(pos)
                  if (vh == null) playback?.unbind()
                }
                playback = null
              }
            }
          })
        }

    (this.videoOverlay as MotionLayout).setTransitionListener(this)
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    if (savedInstanceState != null) restoreState()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    selectionTracker?.addObserver(object : SelectionObserver<String>() {
      override fun onItemStateChanged(
        key: String,
        selected: Boolean
      ) {
        if (selected) {
          // viewModel!!.liveData.value = Triple(key, selected, playback?.tag as String?)
          overlaySheet?.state = STATE_EXPANDED
          @Suppress("UNCHECKED_CAST")
          (kohii.findPlayable(key) as? Playable<PlayerView>)?.bind(
              this@OverlayViewFragment,
              overlayPlayerView,
              Playback.PRIORITY_HIGH
          ) {
            playback = it
          }
        }
      }
    })
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    selectionTracker?.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerView.adapter = null
  }

  private fun restoreState() {
    if (overlaySheet?.state == BottomSheetBehavior.STATE_COLLAPSED) {
      (videoOverlay as? MotionLayout)?.progress = 1F
    } else if (overlaySheet?.state == BottomSheetBehavior.STATE_EXPANDED) {
      (videoOverlay as? MotionLayout)?.progress = 0F
    }

    val selected = selectionTracker?.selection?.firstOrNull()
    if (selected != null) {
      @Suppress("UNCHECKED_CAST")
      (kohii.findPlayable(selected) as? Playable<PlayerView>)?.bind(
          this@OverlayViewFragment,
          overlayPlayerView,
          Playback.PRIORITY_HIGH
      ) { pk ->
        playback = pk
      }
    }
  }

  // MotionLayout.TransitionListener

  override fun onTransitionChange(
    motionLayout: MotionLayout,
    startId: Int,
    endId: Int,
    progress: Float
  ) {
    overlayPlayerView.useController = progress < 0.1
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

  override fun provideContainers(): Array<Any>? {
    return arrayOf(recyclerView, video_player_container)
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}