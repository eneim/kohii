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

package kohii.dev

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import androidx.core.view.contains
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.STARTED
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max

open class Playback<CONTAINER : ViewGroup>(
  internal val manager: Manager,
  internal val host: Host,
  val container: CONTAINER
) {

  companion object {
    internal val CENTER_X: Comparator<Token> = Comparator { o1, o2 ->
      compareValues(o1.containerRect.centerX(), o2.containerRect.centerX())
    }

    internal val CENTER_Y: Comparator<Token> = Comparator { o1, o2 ->
      compareValues(o1.containerRect.centerY(), o2.containerRect.centerY())
    }

    internal val VERTICAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, Host.VERTICAL)
    }

    internal val HORIZONTAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, Host.HORIZONTAL)
    }

    internal val BOTH_AXIS_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, Host.BOTH_AXIS)
    }
  }

  class Token(
    @FloatRange(from = -1.0, to = 1.0)
    val areaOffset: Float, // -1 ~ < 0 : inactive or detached, 0 ~ 1: active
    val containerRect: Rect
  ) {

    fun shouldPrepare(): Boolean {
      return areaOffset >= 0
    }

    fun shouldPlay(): Boolean {
      return areaOffset >= 0.65
    }
  }

  internal val callbacks by lazy(NONE) { linkedSetOf<Callback>() }

  internal fun onAdded() {
    callbacks.forEach { it.onAdded(this) }
    host.registerContainer(this.container)
  }

  internal fun onRemoved() {
    host.unregisterContainer(this.container)
    callbacks.forEach { it.onRemoved(this) }
  }

  internal fun onAttached() {
    //
  }

  internal fun onDetached() {
    //
  }

  open fun <RENDERER : Any> onAttachRenderer(renderer: RENDERER?) {
    if (renderer is View && renderer !== container) {
      container.addView(renderer)
    }
  }

  open fun <RENDERER : Any> onDetachRenderer(renderer: RENDERER?) {
    if (renderer is View && container.contains(renderer)) {
      container.removeView(renderer)
    }
  }

  internal fun onActive() {
    callbacks.forEach { it.onActive(this) }
  }

  internal fun onInActive() {
    callbacks.forEach { it.onInActive(this) }
  }

  internal var lifecycleState: State = State.INITIALIZED

  internal val token: Token
    get() {
      val containerRect = Rect()
      if (!lifecycleState.isAtLeast(STARTED)) return Token(-1F, containerRect)
      if (!ViewCompat.isAttachedToWindow(container)) {
        return Token(-1F, containerRect)
      }

      val visible = container.getGlobalVisibleRect(containerRect)
      if (!visible) return Token(-1F, containerRect)

      val drawArea = with(Rect()) {
        container.getDrawingRect(this)
        width() * height()
      }

      val offset: Float =
        if (drawArea > 0)
          (containerRect.width() * containerRect.height()) / drawArea.toFloat()
        else
          0F
      return Token(offset, containerRect)
    }

  internal fun compareWith(
    other: Playback<*>,
    orientation: Int
  ): Int {
    val thisToken = this.token
    val thatToken = other.token

    val compareVertically by lazy(NONE) { CENTER_Y.compare(thisToken, thatToken) }
    val compareHorizontally by lazy(NONE) { CENTER_X.compare(thisToken, thatToken) }

    var result = when (orientation) {
      Host.VERTICAL -> compareVertically
      Host.HORIZONTAL -> compareHorizontally
      Host.BOTH_AXIS -> max(compareVertically, compareHorizontally)
      Host.NONE_AXIS -> max(compareVertically, compareHorizontally)
      else -> 0
    }

    if (result == 0) result = compareValues(thisToken.areaOffset, thatToken.areaOffset)
    return result
  }

  interface Callback {

    fun onActive(playback: Playback<*>)

    fun onInActive(playback: Playback<*>)

    fun onAdded(playback: Playback<*>)

    fun onRemoved(playback: Playback<*>)
  }
}
