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
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Controller
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.InfinityDialogFragment
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.common.requireWindow
import kohii.v1.sample.ui.fbook.player.PlayerPanel.Callback
import kotlinx.android.synthetic.main.fragment_fbook_player.minimizeButton
import kotlinx.android.synthetic.main.fragment_fbook_player.playerContainer
import kotlinx.android.synthetic.main.fragment_fbook_player.playerView
import java.util.concurrent.atomic.AtomicInteger

class BigPlayerDialog : InfinityDialogFragment(),
    PlayerPanel,
    Playback.Callback,
    Playback.StateListener {

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

  private lateinit var kohii: Kohii
  private lateinit var rebinderFromArgs: Rebinder

  @Suppress("MemberVisibilityCanBePrivate")
  var floatPlayerController: FloatPlayerController? = null

  @Suppress("MemberVisibilityCanBePrivate")
  var playerCallback: Callback? = null

  private val systemUiOptions by lazy {
    AtomicInteger(
        requireActivity().window.decorView.systemUiVisibility
    )
  }

  override val rebinder: Rebinder
    get() = this.rebinderFromArgs

  override fun onAttach(context: Context) {
    super.onAttach(context)
    this.playerCallback = parentFragment as? Callback
    this.floatPlayerController = parentFragment as? FloatPlayerController
  }

  override fun onDetach() {
    super.onDetach()
    this.floatPlayerController = null
    this.playerCallback = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Orientation change will cause this fragment to be recreated, we don't want that to happen.
    // retainInstance = true
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
    kohii.register(this)
        .addBucket(playerContainer)

    requireArguments().apply {
      rebinderFromArgs = requireNotNull(getParcelable(KEY_REBINDER))
      val ratio = getFloat(KEY_RATIO, 16 / 9F)
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

    rebinder.with {
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true

        override fun kohiiCanPause(): Boolean = true

        override fun setupRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            renderer.useController = true
            renderer.setControlDispatcher(kohii.createControlDispatcher(playback))
          }
        }
      }
      callbacks += this@BigPlayerDialog
    }
        .bind(kohii, playerView) {
          it.addStateListener(this@BigPlayerDialog)
        }

    minimizeButton.setOnClickListener {
      floatPlayerController?.showFloatPlayer(rebinder)
      dismissAllowingStateLoss()
    }
  }

  override fun onActive(playback: Playback) {
    playerCallback?.onPlayerActive(this, playback)
  }

  override fun onInActive(playback: Playback) {
    requireActivity().also {
      val decorView = it.window.decorView
      decorView.systemUiVisibility = systemUiOptions.get()
    }
    playerCallback?.onPlayerInActive(this, playback)
  }

  override fun onEnded(playback: Playback) {
    playback.removeStateListener(this)
    dismissAllowingStateLoss()
  }
}
