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
import kotlin.math.max

/**
 * @author eneim (2018/06/24).
 */
open class ViewPlayback<V : View>(
  kohii: Kohii,
  playable: Playable<V>,
  manager: PlaybackManager,
  container: Container,
  target: V,
  options: Config
) : Playback<V>(
    kohii,
    playable,
    manager,
    container,
    target,
    options
) {

  // For debugging purpose only.
  private val debugListener: PlaybackEventListener by lazy {
    object : PlaybackEventListener {
      override fun onFirstFrameRendered() {
        Log.d(TAG, "first frame: ${this@ViewPlayback}")
      }

      override fun onBuffering(playWhenReady: Boolean) {
        Log.d(TAG, "buffering: ${this@ViewPlayback}")
      }

      override fun beforePlay() {
        Log.w(TAG, "beforePlay: ${this@ViewPlayback}")
        target.keepScreenOn = true
      }

      override fun onPlaying() {
        Log.d(TAG, "playing: ${this@ViewPlayback}")
      }

      override fun onPaused() {
        Log.w(TAG, "paused: ${this@ViewPlayback}")
      }

      override fun afterPause() {
        Log.w(TAG, "afterPause: ${this@ViewPlayback}")
        target.keepScreenOn = false
      }

      override fun onCompleted() {
        Log.d("Kohii:PB", "ended: ${this@ViewPlayback}")
        target.keepScreenOn = false
      }
    }
  }

  // TODO [20190112] deal with scaled/transformed View and/or its Parent.
  override val token: ViewToken
    get() {
      val playerRect = Rect()
      val visible = target.getGlobalVisibleRect(playerRect, Point())
      if (!visible) return ViewToken(this, playerRect, -1F)

      val drawRect = Rect()
      target.getDrawingRect(drawRect)
      val drawArea = drawRect.width() * drawRect.height()

      var offset = 0f
      if (drawArea > 0) {
        val visibleArea = playerRect.height() * playerRect.width()
        offset = visibleArea / drawArea.toFloat()
      }

      return ViewToken(this, playerRect, offset)
    }

  @CallSuper
  override fun onAdded() {
    super.onAdded()
    if (BuildConfig.DEBUG) super.addPlaybackEventListener(this.debugListener)
  }

  override fun onRemoved() {
    if (BuildConfig.DEBUG) super.removePlaybackEventListener(this.debugListener)
    super.onRemoved()
  }

  override fun compareWidth(
    other: Playback<*>,
    orientation: Int
  ): Int {
    if (other !is ViewPlayback) {
      // Either 1 or -1.
      return 1 or this.config.priority.compareTo(other.config.priority)
    }

    val thisToken = this.token
    val thatToken = other.token

    val vertical by lazy { CENTER_Y.compare(thisToken, thatToken) }
    val horizontal by lazy { CENTER_X.compare(thisToken, thatToken) }

    var result = this.config.priority.compareTo(other.config.priority)
    if (result == 0) {
      result = when (orientation) {
        Container.VERTICAL -> vertical
        Container.HORIZONTAL -> horizontal
        Container.BOTH_AXIS -> max(vertical, horizontal) // FIXME or closer to center?
        Container.NONE_AXIS -> max(vertical, horizontal) // FIXME or closer to center?
        else -> 0
      }
    }

    if (result == 0) result = compareValues(thisToken.areaOffset, thatToken.areaOffset)
    return result
  }

  // Location on screen, with visible offset within target's parent.
  data class ViewToken internal constructor(
    internal val owner: Playback<*>,
    internal val viewRect: Rect,
    internal val areaOffset: Float,
    internal val canRelease: Boolean = true
  ) : Token() {
    override fun compareTo(other: Token): Int {
      return (other as? ViewToken)?.let {
        var result = this.owner.config.priority.compareTo(other.owner.config.priority)
        if (result == 0) result = CENTER_Y.compare(this, other)
        if (result == 0) result = this.areaOffset.compareTo(other.areaOffset)
        result
      } ?: super.compareTo(other)
    }

    override fun shouldPrepare(): Boolean {
      return areaOffset >= 0f
    }

    override fun shouldPlay(): Boolean {
      return areaOffset >= owner.config.threshold
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