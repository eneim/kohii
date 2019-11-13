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

package kohii.v1.yt1

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener
import com.google.android.youtube.player.YouTubePlayerContainerView
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp

class YouTubePlayerFragment : BaseFragment() {

  companion object {
    const val STATE_KEY = "kohii.v1.yt1.YouTubePlayerFragment.KEY_PLAYER_VIEW_STATE"

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
    val playerState = savedInstanceState?.getBundle(STATE_KEY)
    containerView.initPlayer(viewLifecycleOwner, playerState)
    containerView.doOnLayout {
      this.initializedListener?.let {
        this.initialize(it)
        this.initializedListener = null
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    containerView.playerState?.also {
      outState.putBundle(STATE_KEY, it)
    }
  }

  private var initializedListener: OnInitializedListener? = null

  fun initialize(initializedListener: OnInitializedListener) {
    if (::containerView.isInitialized) {
      containerView.initialize(getApp().youtubeApiKey, initializedListener)
      this.initializedListener = null
    } else {
      this.initializedListener = initializedListener
    }
  }

  fun allowedToPlay(): Boolean {
    return this.isVisible && visibleAreaOffset(this.view) >= 0.99F
  }

  override fun onDestroyView() {
    this.initializedListener = null
    super.onDestroyView()
  }
}
