/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.ViewCompat
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max

/**
 * @author eneim (2018/06/24).
 */
open class ViewPlayback<V : View, OUTPUT : Any>(
  kohii: Kohii,
  playable: Playable<OUTPUT>,
  manager: PlaybackManager,
  target: V,
  options: Config
) : Playback<OUTPUT>(
    kohii,
    playable,
    manager,
    target,
    options
) {

  private val keepScreenOnListener by lazy(NONE) {
    object : PlaybackEventListener {

      override fun beforePlay(playback: Playback<*>) {
        target.keepScreenOn = true
      }

      override fun afterPause(playback: Playback<*>) {
        target.keepScreenOn = false
      }

      override fun onEnd(playback: Playback<*>) {
        target.keepScreenOn = false
      }
    }
  }

  // For debugging purpose only.
  private val debugListener: PlaybackEventListener by lazy {
    object : PlaybackEventListener {
      override fun onFirstFrameRendered(playback: Playback<*>) {
        Log.d(TAG, "first frame: ${this@ViewPlayback}")
      }

      override fun onBuffering(
        playback: Playback<*>,
        playWhenReady: Boolean
      ) {
        Log.d(TAG, "buffering: ${this@ViewPlayback}")
      }

      override fun beforePlay(playback: Playback<*>) {
        Log.w(TAG, "beforePlay: ${this@ViewPlayback}")
      }

      override fun onPlay(playback: Playback<*>) {
        Log.d(TAG, "playing: ${this@ViewPlayback}")
      }

      override fun onPause(playback: Playback<*>) {
        Log.w(TAG, "paused: ${this@ViewPlayback}")
      }

      override fun afterPause(playback: Playback<*>) {
        Log.w(TAG, "afterPause: ${this@ViewPlayback}")
      }

      override fun onEnd(playback: Playback<*>) {
        Log.d(TAG, "ended: ${this@ViewPlayback}")
      }
    }
  }

  // TODO [20190112] deal with scaled/transformed View and/or its Parent.
  override val token: ViewToken
    get() {
      val viewTarget = target as View
      val playerRect = Rect()
      if (!ViewCompat.isAttachedToWindow(viewTarget)) {
        return ViewToken(this.config, playerRect, -1F)
      }

      val visible = viewTarget.getGlobalVisibleRect(playerRect, Point())
      if (!visible) return ViewToken(this.config, playerRect, -1F)

      val drawRect = Rect()
      viewTarget.getDrawingRect(drawRect)
      val drawArea = drawRect.width() * drawRect.height()

      var offset = 0f
      if (drawArea > 0) {
        val visibleArea = playerRect.height() * playerRect.width()
        offset = visibleArea / drawArea.toFloat()
      }

      return ViewToken(this.config, playerRect, offset)
    }

  @CallSuper
  override fun onAdded() {
    super.onAdded()
    if (config.keepScreenOn) super.addPlaybackEventListener(keepScreenOnListener)
    if (BuildConfig.DEBUG) super.addPlaybackEventListener(this.debugListener)
  }

  @CallSuper
  override fun onRemoved() {
    if (BuildConfig.DEBUG) super.removePlaybackEventListener(this.debugListener)
    if (config.keepScreenOn) super.removePlaybackEventListener(keepScreenOnListener)
    super.onRemoved()
  }

  override fun compareWidth(
    other: Playback<*>,
    orientation: Int
  ): Int {
    if (other !is ViewPlayback<*, *>) {
      return 0
    }

    val thisToken = this.token
    val thatToken = other.token

    val verticalOrder by lazy { CENTER_Y.compare(thisToken, thatToken) }
    val horizontalOrder by lazy { CENTER_X.compare(thisToken, thatToken) }

    var result = when (orientation) {
      TargetHost.VERTICAL -> verticalOrder
      TargetHost.HORIZONTAL -> horizontalOrder
      TargetHost.BOTH_AXIS -> max(verticalOrder, horizontalOrder)
      TargetHost.NONE_AXIS -> max(verticalOrder, horizontalOrder)
      else -> 0
    }

    if (result == 0) result = compareValues(thisToken.areaOffset, thatToken.areaOffset)
    return result
  }

  @Suppress("UNCHECKED_CAST")
  override val outputHolder: OUTPUT?
    get() = this.target as? OUTPUT

  // Location on screen, with visible offset within target's parent.
  data class ViewToken constructor(
    val config: Config,
    val viewRect: Rect,
    val areaOffset: Float
  ) : Token() {

    override fun shouldPrepare(): Boolean {
      return areaOffset >= 0f
    }

    override fun shouldPlay(): Boolean {
      return areaOffset >= config.threshold
    }

    override fun toString(): String {
      return "Token::$viewRect::$areaOffset"
    }
  }

  companion object {
    val CENTER_Y: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.viewRect.centerY(), o2.viewRect.centerY())
    }

    val CENTER_X: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.viewRect.centerX(), o2.viewRect.centerX())
    }
  }
}
