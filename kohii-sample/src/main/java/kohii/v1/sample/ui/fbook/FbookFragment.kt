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

package kohii.v1.sample.ui.fbook

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import kohii.v1.core.Common
import kohii.v1.core.Manager
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.core.Scope
import kohii.v1.exoplayer.Kohii
import kohii.v1.media.VolumeInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.checkOverlayPermission
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.ui.fbook.player.BigPlayerDialog
import kohii.v1.sample.ui.fbook.player.FloatPlayerController
import kohii.v1.sample.ui.fbook.player.PlayerPanel
import kohii.v1.sample.ui.fbook.vh.VideoViewHolder
import kotlinx.android.synthetic.main.fragment_facebook.content
import kotlinx.android.synthetic.main.fragment_facebook.dummyPlayer
import kotlinx.android.synthetic.main.fragment_facebook.recyclerView
import kotlin.properties.Delegates

/**
 * A demonstration that implements the UX of Facebook Videos.
 */
class FbookFragment : BaseFragment(),
    BackPressConsumer,
    FloatPlayerController,
    PlayerPanel.Callback, Manager.OnSelectionListener, Playback.StateListener {

  companion object {
    private const val STATE_KEY_REBINDER = "kohii::fbook::arg::rebinder"
    private const val PERMISSION_REQ_CODE = 123

    fun newInstance() = FbookFragment()
  }

  private lateinit var kohii: Kohii

  private val viewModel: FbookViewModel by viewModels()

  private var currentSelectedRebinder: Rebinder? = null
  private var currentSelectedPlayback: Playback? by Delegates.observable<Playback?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        from?.removeStateListener(this@FbookFragment)
        if (to != null) {
          to.addStateListener(this@FbookFragment)
          currentSelectedRebinder = kohii.fetchRebinder(to.tag)
        }
      }
  )

  // Floating View
  private val floatPlayerManager by lazy { FloatPlayerManager(requireActivity()) }

  private var rebindAction: (() -> Unit)? = null
  private var overlayPlayback: Playback? = null

  private var currentOverlayRebinder: Rebinder? = null
  private var currentOverlayPlayerInfo by Delegates.observable<OverlayPlayerInfo?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (to == null) { // dismiss the overlay player.
          if (from != null) {
            currentOverlayRebinder = null
            closeFullscreenPlayer(from.rebinder)
            closeFloatPlayer(from.rebinder)
            clearPlayerSelection(from.rebinder)
          }
        } else { // open overlay player
          val (mode, rebinder) = to
          currentOverlayRebinder = rebinder
          when (mode) {
            OverlayPlayerInfo.MODE_FULLSCREEN -> openFullscreenPlayer(rebinder)
            OverlayPlayerInfo.MODE_FLOAT -> openFloatPlayer(rebinder)
            else -> throw IllegalArgumentException("Unknown overlay mode: $mode")
          }
        }
      })

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_facebook, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    val manager = kohii.register(this)
        .addBucket(recyclerView)
        .addBucket(content)

    viewModel.apply {
      timelineVolume.observe(viewLifecycleOwner) {
        manager.applyVolumeInfo(it, recyclerView, Scope.BUCKET)
        val adapter = recyclerView.adapter
        if (adapter != null) {
          recyclerView.filterVisibleHolder<VideoViewHolder>()
              .forEach { vh -> adapter.notifyItemChanged(vh.adapterPosition, it) }
        }
      }

      overlayPlayerInfo.observe(viewLifecycleOwner) {
        currentOverlayPlayerInfo = it
      }
    }

    val videos = getApp().videos
    val adapter = FbookAdapter(kohii, videos, this,
        shouldBindVideo = { rebinder -> currentOverlayRebinder != rebinder },
        volumeClick = {
          val current = requireNotNull(viewModel.timelineVolume.value)
          viewModel.timelineVolume.value =
            VolumeInfo(!current.mute, current.volume)
        }
    )

    recyclerView.adapter = adapter

    val savedBinder: Rebinder? = savedInstanceState?.getParcelable(STATE_KEY_REBINDER)
    if (savedBinder != null) {
      if (requireActivity().isLandscape()) {
        val info = OverlayPlayerInfo(OverlayPlayerInfo.MODE_FULLSCREEN, savedBinder)
        recyclerView.doOnLayout {
          viewModel.overlayPlayerInfo.value = info
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (currentSelectedRebinder != null) {
      outState.putParcelable(STATE_KEY_REBINDER, currentSelectedRebinder)
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PERMISSION_REQ_CODE) {
      if (checkOverlayPermission()) dispatchOpenFloatPlayer()
    }
  }

  override fun onDestroyView() {
    recyclerView.adapter = null
    super.onDestroyView()
  }

  override fun onDestroy() {
    super.onDestroy()
    rebindAction = null
    this.floatPlayerManager.closeFloatPlayer { /* do nothing */ }
  }

  // Manager.OnSelectionListener

  override fun onSelection(selection: Collection<Playback>) {
    currentSelectedPlayback = selection.firstOrNull()
  }

  // FloatPlayerController

  override fun showFloatPlayer(rebinder: Rebinder) {
    val overlayPlayerInfo = OverlayPlayerInfo(OverlayPlayerInfo.MODE_FLOAT, rebinder)
    viewModel.overlayPlayerInfo.value = overlayPlayerInfo
  }

  // PlayerPanel.Callback

  override fun onPlayerActive(
    player: PlayerPanel,
    playback: Playback
  ) {
    overlayPlayback = playback
  }

  // Called when BigPlayerDialog is dismissed.
  override fun onPlayerInActive(
    player: PlayerPanel,
    playback: Playback
  ) {
    if (this.currentOverlayPlayerInfo?.mode != OverlayPlayerInfo.MODE_FLOAT) {
      // Not a 'Close Dialog player while opening Float player' action
      // = this is a natural Dialog closing due to a back-press or other User interactions
      viewModel.overlayPlayerInfo.value = null
    }
  }

  override fun requestDismiss(panel: PlayerPanel) {
    // Wait for the RecyclerView to finish its first layout.
    recyclerView.doOnLayout {
      if (panel is DialogFragment) panel.dismissAllowingStateLoss()
    }
  }

  // Playback.PlaybackListener

  override fun onEnded(playback: Playback) {
    super.onEnded(playback)
    if (playback === currentSelectedPlayback) {
      playback.removeStateListener(this)
      currentSelectedPlayback = null
      currentSelectedRebinder = null
    }
  }

  // BackPressConsumer

  override fun consumeBackPress(): Boolean {
    return if (viewModel.overlayPlayerInfo.value != null) {
      viewModel.overlayPlayerInfo.value = null
      true
    } else false
  }

  // Other util methods

  private fun clearPlayerSelection(rebinder: Rebinder) {
    val adapter = recyclerView.adapter
    if (adapter != null) {
      recyclerView.filterVisibleHolder<VideoViewHolder> { it.rebinder == rebinder }
          .also { if (it.isEmpty()) overlayPlayback?.unbind() } // No visible Player --> unbind.
          .forEach {
            adapter.notifyItemChanged(it.adapterPosition)
          }
    }
    overlayPlayback = null
  }

  private fun openFullscreenPlayer(rebinder: Rebinder) {
    val player = BigPlayerDialog.newInstance(rebinder, 16 / 9.toFloat())
    player.show(childFragmentManager, rebinder.tag.toString())
  }

  private fun closeFullscreenPlayer(rebinder: Rebinder) {
    val dialog = childFragmentManager.findFragmentByTag(rebinder.tag.toString())
    if (dialog is BigPlayerDialog) dialog.dismissAllowingStateLoss()
  }

  @SuppressLint("InlinedApi")
  private fun openFloatPlayer(rebinder: Rebinder) {
    if (!floatPlayerManager.floating.get()) {
      rebindAction = {
        floatPlayerManager.openFloatPlayer { playerView ->
          currentOverlayPlayerInfo?.rebinder
              ?.with { repeatMode = Common.REPEAT_MODE_OFF }
              ?.bind(kohii, playerView) { playback ->
                dummyPlayer.isVisible = false // View.GONE
                playback.addStateListener(object : Playback.StateListener {
                  override fun onEnded(playback: Playback) {
                    kohii.unstick(playback)
                    playback.removeStateListener(this)
                    viewModel.overlayPlayerInfo.value = null
                  }
                })
                kohii.stick(playback)
                overlayPlayback = playback
              }
        }
      }

      if (checkOverlayPermission()) {
        dummyPlayer.isVisible = false // View.GONE
        dispatchOpenFloatPlayer()
      } else {
        dummyPlayer.isInvisible = true // View.INVISIBLE
        rebinder.bind(kohii, dummyPlayer)
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${requireActivity().packageName}".toUri()
        )
        startActivityForResult(intent, PERMISSION_REQ_CODE)
      }
    }
  }

  private fun dispatchOpenFloatPlayer() {
    recyclerView.doOnLayout {
      rebindAction?.invoke()
      rebindAction = null
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun closeFloatPlayer(rebinder: Rebinder) {
    floatPlayerManager.closeFloatPlayer {}
  }
}
