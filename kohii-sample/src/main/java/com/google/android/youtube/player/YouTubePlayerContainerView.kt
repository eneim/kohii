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

package com.google.android.youtube.player

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class YouTubePlayerContainerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    YouTubePlayer.Provider,
    LifecycleObserver {

  internal var activity: Activity =
    context as? Activity ?: throw IllegalArgumentException("Need Activity")

  internal var playerView: YouTubePlayerView? = null

  val playerState: Bundle?
    get() = playerView?.e()

  fun initPlayer(
    lifecycleOwner: LifecycleOwner,
    playerState: Bundle?
  ) {
    val playerView = YouTubePlayerView(activity, null, 0,
        object : YouTubePlayerView.b {
          override fun a(
            view: YouTubePlayerView,
            apiKey: String,
            onInitializedListener: YouTubePlayer.OnInitializedListener
          ) {
            view.a(activity, view, apiKey, onInitializedListener, playerState)
          }

          override fun a(youTubePlayerView: YouTubePlayerView) {}
        })
    val params = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    params.gravity = Gravity.CENTER
    super.addView(playerView, 0, params)
    lifecycleOwner.lifecycle.addObserver(this)
    this.playerView = playerView
  }

  override fun initialize(
    apiKey: String,
    onInitializedListener: YouTubePlayer.OnInitializedListener?
  ) {
    if (this.playerView != null) this.playerView!!.initialize(apiKey, onInitializedListener)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  internal fun onStart() {
    if (this.playerView != null) playerView!!.a()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  internal fun onResume() {
    if (this.playerView != null) playerView!!.b()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  internal fun onPause() {
    if (this.playerView != null) playerView!!.c()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  internal fun onStop() {
    if (this.playerView != null) playerView!!.d()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    owner.lifecycle.removeObserver(this)
    Log.w("Kohii::YTT", "destroyed: $playerView")
    this.playerView?.let {
      it.c(activity.isFinishing)
      it.removeAllViews()
    }
  }
}
