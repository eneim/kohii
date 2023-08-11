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

package kohii.v1.sample.ui.combo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
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
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.InitData
import kohii.v1.sample.databinding.FragmentPlayerHorizontalBinding
import java.util.concurrent.atomic.AtomicInteger

class LandscapeFullscreenFragment : BaseFragment() {

  companion object {
    private const val KEY_INIT_DATA = "kohii:fragment:player:init_data"
    private const val KEY_REBINDER = "kohii:fragment:player:rebinder"

    fun newInstance(
      rebinder: Rebinder,
      initData: InitData
    ): LandscapeFullscreenFragment {
      val bundle = Bundle().also {
        it.putParcelable(KEY_REBINDER, rebinder)
        it.putParcelable(KEY_INIT_DATA, initData)
      }
      return LandscapeFullscreenFragment().also { it.arguments = bundle }
    }
  }

  private val activityOrientation = AtomicInteger(Int.MIN_VALUE)
  private val displayRotation = AtomicInteger(0)
  private val systemUiOption by lazy {
    AtomicInteger(requireActivity().window.decorView.systemUiVisibility)
  }

  private var callback: Callback? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    callback = activity as? Callback
  }

  override fun onDetach() {
    super.onDetach()
    callback = null
  }

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Orientation change will cause this fragment to be recreated, we don't want that to happen.
    retainInstance = true
    // Save current Activity info
    requireActivity().also {
      activityOrientation.set(it.requestedOrientation)
      val decorView = it.window.decorView
      systemUiOption.set(decorView.systemUiVisibility)
      displayRotation.set(it.windowManager.defaultDisplay.rotation)

      // Request Landscape
      it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_player_horizontal, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val binding: FragmentPlayerHorizontalBinding = FragmentPlayerHorizontalBinding.bind(view)
    val container =
      binding.playerView.findViewById(R.id.exo_content_frame) as AspectRatioFrameLayout

    val (initData, rebinder) = requireArguments().let {
      requireNotNull(it.getParcelable<InitData>(KEY_INIT_DATA)) to
        requireNotNull(it.getParcelable<Rebinder>(KEY_REBINDER))
    }

    container.setAspectRatio(initData.aspectRatio)

    val kohii = Kohii[this]
    kohii.register(this)
      .addBucket(binding.playerContainer)
    rebinder.with {
      controller = object : Controller {
        override fun kohiiCanStart(): Boolean = true

        override fun kohiiCanPause(): Boolean = true

        override fun setupRenderer(playback: Playback, renderer: Any?) {
          if (renderer is PlayerView) {
            // TODO: replace with custom ForwardingPlayer.
            // renderer.useController = true
            // renderer.setControlDispatcher(kohii.createControlDispatcher(playback))
          }
        }
      }
    }
      .bind(kohii, binding.playerView)

    (requireActivity() as AppCompatActivity).also {
      if (it.windowManager.defaultDisplay.rotation % 2 == 1) {
        // apply new UI config
        // it.supportActionBar?.hide()
        callback?.hideToolbar()
        val currentOptions = it.window.decorView.systemUiVisibility
        it.window.decorView.systemUiVisibility = (
          currentOptions
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
        // it.supportActionBar?.show()
        callback?.showToolbar()
        it.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      }
    }
  }

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onDestroy() {
    (requireActivity() as AppCompatActivity).also {
      // check previous rotation
      // val rotation = displayRotation.get()
      it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      // then restore previous orientation
      it.requestedOrientation = activityOrientation.get()
      val decorView = it.window.decorView
      decorView.systemUiVisibility = systemUiOption.get()
      // it.supportActionBar?.show()
      callback?.showToolbar()
    }
    super.onDestroy()
  }

  interface Callback {

    fun hideToolbar()

    fun showToolbar()
  }
}
