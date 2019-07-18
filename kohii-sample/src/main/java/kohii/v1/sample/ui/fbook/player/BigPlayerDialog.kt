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

package kohii.v1.sample.ui.fbook.player

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.Rebinder
import kohii.v1.exo.DefaultControlDispatcher
import kohii.v1.sample.R
import kohii.v1.sample.common.InfinityDialogFragment
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.common.requireWindow
import kotlinx.android.synthetic.main.fragment_fbook_player.minimizeButton
import kotlinx.android.synthetic.main.fragment_fbook_player.playerContainer
import kotlinx.android.synthetic.main.fragment_fbook_player.playerView
import java.util.concurrent.atomic.AtomicInteger

class BigPlayerDialog : InfinityDialogFragment(), PlayerPanel, Playback.Callback {

  companion object {
    private const val KEY_REBINDER = "kohii:fragment:player:rebinder"
    private const val KEY_RATIO = "kohii:fragment:player:ratio"

    fun newInstance(
      rebinder: Rebinder,
      ratio: Float
    ) = BigPlayerDialog().also {
      val args = Bundle()
      args.putParcelable(KEY_REBINDER, rebinder)
      args.putFloat(KEY_RATIO, ratio)
      it.arguments = args
    }
  }

  lateinit var kohii: Kohii
  private lateinit var rebinderFromArgs: Rebinder

  var floatPlayerController: FloatPlayerController? = null
  var playerCallback: PlayerPanel.Callback? = null

  private val systemUiOptions by lazy {
    AtomicInteger(
        requireActivity().window.decorView.systemUiVisibility
    )
  }

  override val rebinder: Rebinder
    get() = this.rebinderFromArgs

  override fun onAttach(context: Context) {
    super.onAttach(context)
    parentFragment?.let {
      if (it is PlayerPanel.Callback) this.playerCallback = it
      if (it is FloatPlayerController) this.floatPlayerController = it
    }
  }

  override fun onDetach() {
    super.onDetach()
    this.floatPlayerController = null
    this.playerCallback = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Orientation change will cause this fragment to be recreated, we don't want that to happen.
    retainInstance = true
    (requireActivity() as AppCompatActivity).also {
      val decorView = it.window.decorView
      systemUiOptions.set(decorView.systemUiVisibility)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_fbook_player, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    val manager = kohii.register(this, playerContainer)

    requireArguments().apply {
      rebinderFromArgs = getParcelable(KEY_REBINDER) as Rebinder
      val ratio = getFloat(KEY_RATIO, 16 / 9.toFloat())
      val container = playerView.findViewById(R.id.exo_content_frame) as AspectRatioFrameLayout
      container.setAspectRatio(ratio)
    }

    playerView.setControllerVisibilityListener {
      minimizeButton.visibility = it
    }

    val decorView = requireWindow().decorView
    if (requireActivity().isLandscape()) {
      val currentUiOptions = decorView.systemUiVisibility
      decorView.systemUiVisibility = (
          currentUiOptions
              or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              or View.SYSTEM_UI_FLAG_IMMERSIVE
              // or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              // or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              // or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              // Hide the nav bar and status bar
              or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              or View.SYSTEM_UI_FLAG_FULLSCREEN
          )
    } else {
      decorView.systemUiVisibility = 0
      playerCallback?.requestDismiss(this) ?: dismissAllowingStateLoss()
    }

    rebinder
        .with {
          controller = DefaultControlDispatcher(manager, playerView)
          callback = this@BigPlayerDialog
        }
        .rebind(kohii, playerView) {
          it.addPlaybackEventListener(object : PlaybackEventListener {
            override fun onEnd(playback: Playback<*>) {
              playback.removePlaybackEventListener(this)
              dismissAllowingStateLoss()
            }
          })
        }

    minimizeButton.setOnClickListener {
      floatPlayerController?.showFloatPlayer(rebinder)
      dismissAllowingStateLoss()
    }
  }

  override fun onActive(playback: Playback<*>) {
    playerCallback?.onPlayerActive(this, playback)
  }

  override fun onInActive(playback: Playback<*>) {
    requireActivity().also {
      val decorView = it.window.decorView
      decorView.systemUiVisibility = systemUiOptions.get()
    }
    playerCallback?.onPlayerInActive(this, playback)
  }
}
