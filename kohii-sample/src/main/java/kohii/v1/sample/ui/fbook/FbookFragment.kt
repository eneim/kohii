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

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.OnSelectionCallback
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.Rebinder
import kohii.v1.Scope
import kohii.v1.TargetHost
import kohii.v1.TargetHost.Builder
import kohii.v1.sample.R
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.ui.fbook.player.BigPlayerDialog
import kohii.v1.sample.ui.fbook.player.FloatPlayerController
import kohii.v1.sample.ui.fbook.player.PlayerPanel
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder.OnClick
import kohii.v1.sample.ui.fbook.vh.VideoViewHolder
import kotlinx.android.synthetic.main.fragment_facebook.recyclerView
import kotlin.properties.Delegates

/**
 * A demonstration that implements the UX of Facebook Videos.
 */
class FbookFragment : BaseFragment(),
    BackPressConsumer,
    FloatPlayerController,
    PlayerPanel.Callback, PlaybackEventListener {

  companion object {
    private const val STATE_KEY_REBINDER = "kohii::fbook::arg::rebinder"
    private const val PERMISSION_REQ_CODE = 123

    fun newInstance() = FbookFragment()
  }

  // Will use host Activity's ViewModel
  private val viewModel: FbookViewModel by viewModels()

  private lateinit var kohii: Kohii
  private lateinit var rvHost: TargetHost

  private var latestRebinder: Rebinder? = null
  private var latestPlayback: Playback<*>? = null

  // Floating View
  private val floatPlayerManager by lazy { FloatPlayerManager(requireActivity()) }
  private var rebindAction: (() -> Unit)? = null
  private var overlayPlayback: Playback<*>? = null
  private var overlayRebinder: Rebinder? = null

  internal var currentPlayerInfo
      by Delegates.observable<OverlayPlayerInfo?>(null) { _, oldVal, newVal ->
        if (newVal == null) {
          if (oldVal != null) {
            clearPlayerSelection(oldVal.rebinder)
            closeDialogPlayer(oldVal.rebinder)
            closeFloatPlayer(oldVal.rebinder)
          }
        } else {
          val (mode, rebinder) = newVal
          overlayRebinder = rebinder
          when (mode) {
            OverlayPlayerInfo.MODE_DIALOG -> {
              openDialogPlayer(rebinder)
            }
            OverlayPlayerInfo.MODE_FLOAT -> {
              openFloatPlayer(rebinder)
            }
            else -> throw IllegalArgumentException("Unknown overlay mode: $mode")
          }
        }
      }

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
    // Trick to ensure the order of Playback binding.
    // By doing this, the RecyclerView will finish its layout before the BigPlayerFragment being destroyed (if it exists before).
    // Therefore, the ViewHolder will finish the binding before its Playable is released by the destruction of BigPlayerFragment
    postponeEnterTransition()
    recyclerView.doOnLayout {
      startPostponedEnterTransition()
    }

    kohii = Kohii[this]
    val manager = kohii.register(this)
    rvHost = manager.registerTargetHost(Builder(recyclerView))!!
    manager.addOnSelectionCallback(object : OnSelectionCallback {
      override fun onSelection(playbacks: Collection<Playback<*>>) {
        val picked = playbacks.firstOrNull()
        if (latestPlayback !== picked) {
          latestPlayback?.removePlaybackEventListener(this@FbookFragment)
          latestPlayback = picked
        }
        if (picked != null) {
          latestRebinder = kohii.fetchRebinder(picked.tag)
          picked.addPlaybackEventListener(this@FbookFragment)
        }
      }
    })

    viewModel.apply {
      timelineVolume.observe({ viewLifecycleOwner.lifecycle }) {
        kohii.applyVolumeInfo(it, rvHost, Scope.HOST)
        val adapter = recyclerView.adapter
        if (adapter != null) {
          recyclerView.currentVisible<VideoViewHolder>()
              .forEach { vh -> adapter.notifyItemChanged(vh.adapterPosition, it) }
        }
      }

      overlayPlayerInfo.observe({ viewLifecycleOwner.lifecycle }) {
        currentPlayerInfo = it
      }
    }

    val videos = getApp().videos
    val adapter = FbookAdapter(kohii, manager, videos, this, onClick = object : OnClick {
      override fun onClick(
        receiver: View,
        holder: FbookItemHolder
      ) {
        if (holder is VideoViewHolder && receiver === holder.volume) {
          val current = viewModel.timelineVolume.value as VolumeInfo
          viewModel.timelineVolume.value = VolumeInfo(!current.mute, current.volume)
        }
      }
    })

    recyclerView.setHasFixedSize(true)
    recyclerView.adapter = adapter

    val savedBinder = savedInstanceState?.getParcelable(STATE_KEY_REBINDER) as Rebinder?

    if (savedBinder != null) {
      if (requireActivity().isLandscape()) {
        val info = OverlayPlayerInfo(OverlayPlayerInfo.MODE_DIALOG, savedBinder)
        viewModel.overlayPlayerInfo.value = info
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (latestRebinder != null) {
      outState.putParcelable(STATE_KEY_REBINDER, latestRebinder)
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PERMISSION_REQ_CODE) {
      dispatchOpenFloatPlayer()
    }
  }

  override fun onDestroyView() {
    recyclerView.adapter = null
    super.onDestroyView()
  }

  override fun onDestroy() {
    super.onDestroy()
    rebindAction = null
    this.floatPlayerManager.exitPictureInPicture { /* do nothing */ }
  }

  override fun showFloatPlayer(rebinder: Rebinder) {
    val overlayPlayerInfo = OverlayPlayerInfo(OverlayPlayerInfo.MODE_FLOAT, rebinder)
    viewModel.overlayPlayerInfo.value = overlayPlayerInfo
  }

  override fun onPlayerActive(
    player: PlayerPanel,
    playback: Playback<*>
  ) {
    overlayPlayback = playback
  }

  // Called when BigPlayerDialog is dismissed.
  override fun onPlayerInActive(
    player: PlayerPanel,
    playback: Playback<*>
  ) {
    if (this.currentPlayerInfo?.mode != OverlayPlayerInfo.MODE_FLOAT) {
      // Not a 'close Dialog player after opening Float player' action
      // = this is a natural Dialog closing due to a back-press or other User interactions
      viewModel.overlayPlayerInfo.value = null
    }
  }

  // PlaybackEventListener
  override fun onCompleted(playback: Playback<*>) {
    super.onCompleted(playback)
    if (playback === latestPlayback) {
      latestPlayback = null
      latestRebinder = null
    }
  }

  override fun consumeBackPress(): Boolean {
    return if (viewModel.overlayPlayerInfo.value != null) {
      viewModel.overlayPlayerInfo.value = null
      true
    } else false
  }

  private fun clearPlayerSelection(rebinder: Rebinder) {
    recyclerView.adapter?.apply {
      recyclerView.currentVisible<VideoViewHolder> { it.rebinder == rebinder }
          .also { if (it.isEmpty()) overlayPlayback?.unbind() }
          .forEach {
            notifyItemChanged(it.adapterPosition)
          }
    }
    overlayPlayback = null
    overlayRebinder = null
  }

  private fun openDialogPlayer(rebinder: Rebinder) {
    val player = BigPlayerDialog.newInstance(rebinder, 16 / 9.toFloat())
    player.show(childFragmentManager, rebinder.tag)
  }

  private fun closeDialogPlayer(rebinder: Rebinder) {
    val dialog = childFragmentManager.findFragmentByTag(rebinder.tag)
    if (dialog is BigPlayerDialog) dialog.dismissAllowingStateLoss()
  }

  private fun openFloatPlayer(rebinder: Rebinder) {
    if (!floatPlayerManager.floating.get()) {
      rebindAction = {
        floatPlayerManager.enterPictureInPicture { playerView ->
          rebinder.with { repeatMode = Playable.REPEAT_MODE_OFF }
              .rebind(kohii, playerView) { playback ->
                playback.addPlaybackEventListener(object : PlaybackEventListener {
                  override fun onCompleted(playback: Playback<*>) {
                    playback.removePlaybackEventListener(this)
                    viewModel.overlayPlayerInfo.value = null
                  }
                })
                kohii.promote(playback)
                overlayPlayback = playback
              }
        }
      }

      if (VERSION.SDK_INT >= VERSION_CODES.M) {
        if (Settings.canDrawOverlays(requireContext())) {
          dispatchOpenFloatPlayer()
        } else {
          val intent = Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:" + requireActivity().applicationContext.packageName)
          )
          startActivityForResult(intent, PERMISSION_REQ_CODE)
        }
      } else {
        dispatchOpenFloatPlayer()
      }
    }
  }

  private fun dispatchOpenFloatPlayer() {
    recyclerView.doOnLayout {
      rebindAction?.invoke()
      rebindAction = null
    }
  }

  private fun closeFloatPlayer(@Suppress("UNUSED_PARAMETER") rebinder: Rebinder) {
    floatPlayerManager.exitPictureInPicture { }
  }
}
