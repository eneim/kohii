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

package kohii.v1.experiments

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.arrayMapOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleObserver
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener
import com.google.android.youtube.player.YouTubePlayerContainerView

class YouTubePlayerFragment : Fragment() {

  companion object {
    const val STATE_KEY = "kohii.v1.experiments.YouTubePlayerFragment.KEY_PLAYER_VIEW_STATE"

    fun newInstance() = YouTubePlayerFragment()

    private fun visibleAreaOffset(playerView: View?): Float {
      if (playerView == null) return -1F
      val drawRect = Rect()
      playerView.getDrawingRect(drawRect)
      val drawArea = drawRect.width() * drawRect.height()

      val playerRect = Rect()
      val visible = playerView.getGlobalVisibleRect(playerRect, Point())

      var offset = 0f
      if (visible && drawArea > 0) {
        val visibleArea = playerRect.height() * playerRect.width()
        offset = visibleArea / drawArea.toFloat()
      }
      return offset
    }
  }

  private lateinit var containerView: YouTubePlayerContainerView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    containerView = inflater.inflate(
        R.layout.widget_youtube_player, container, false
    ) as YouTubePlayerContainerView
    return containerView
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    observers.forEach { if (it.value) viewLifecycleOwner.lifecycle.addObserver(it.key) }
    val playerState = savedInstanceState?.getBundle(STATE_KEY)
    containerView.initPlayer(viewLifecycleOwner, playerState)
    containerView.doOnLayout {
      this.initializedListener?.let {
        this.initialize(it)
        this.initializedListener = null
      }
    }
  }

  override fun onDestroyView() {
    this.initializedListener = null
    observers.forEach { viewLifecycleOwner.lifecycle.removeObserver(it.key) }
    observers.clear()
    super.onDestroyView()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val saveState = containerView.playerState
    if (saveState != null) outState.putBundle(STATE_KEY, saveState)
  }

  private var initializedListener: OnInitializedListener? = null

  internal fun initialize(initializedListener: OnInitializedListener) {
    val keyId = resources.getIdentifier("google_api_key", "string", requireContext().packageName)
    require(keyId > 0) { "No valid API Key found." }
    this.initialize(getString(keyId), initializedListener)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  internal fun initialize(
    apiKey: String,
    initializedListener: OnInitializedListener
  ) {
    require(apiKey.isNotBlank()) { "No valid API Key found." }
    if (::containerView.isInitialized) {
      containerView.initialize(apiKey, initializedListener)
      this.initializedListener = null
    } else {
      this.initializedListener = initializedListener
    }
  }

  internal fun allowedToPlay(): Boolean {
    return this.isVisible && visibleAreaOffset(this.view) >= 0.999F
  }

  // false --> added when view is available
  // true --> added when view is not available.
  private val observers = arrayMapOf<LifecycleObserver, Boolean>()

  internal fun addLifecycleObserver(observer: LifecycleObserver) {
    if (view != null) {
      observers[observer] = false
      viewLifecycleOwner.lifecycle.addObserver(observer)
    } else observers[observer] = true
  }

  internal fun removeLifecycleObserver(observer: LifecycleObserver) {
    if (observers[observer] == false) {
      viewLifecycleOwner.lifecycle.removeObserver(observer)
    }

    observers.remove(observer)
  }
}
