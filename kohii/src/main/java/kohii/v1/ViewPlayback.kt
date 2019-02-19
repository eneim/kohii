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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import java.util.Comparator
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
open class ViewPlayback<V : View>(
  kohii: Kohii,
  playable: Playable<V>,
  manager: PlaybackManager,
  target: V?,
  priority: Int = PRIORITY_NORMAL,
  delay: () -> Long = Playback.NO_DELAY
) : Playback<V>(
    kohii,
    playable,
    manager,
    target,
    priority,
    delay
), View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

  // For debugging purpose only.
  private val debugListener: PlaybackEventListener by lazy {
    object : PlaybackEventListener {
      override fun onFirstFrameRendered() {
        Log.d("Kohii:PB", "first frame: ${this@ViewPlayback}")
      }

      override fun onBuffering(playWhenReady: Boolean) {
        Log.d("Kohii:PB", "buffering: ${this@ViewPlayback}")
      }

      override fun beforePlay() {
        Log.e("Kohii:PB", "beforePlay: ${this@ViewPlayback}")
        target?.keepScreenOn = true
      }

      override fun onPlaying() {
        Log.d("Kohii:PB", "playing: ${this@ViewPlayback}")
      }

      override fun onPaused() {
        Log.w("Kohii:PB", "paused: ${this@ViewPlayback}")
      }

      override fun afterPause() {
        Log.w("Kohii:PB", "afterPause: ${this@ViewPlayback}")
        target?.keepScreenOn = false
      }

      override fun onCompleted() {
        Log.d("Kohii:PB", "ended: ${this@ViewPlayback}")
        target?.keepScreenOn = false
      }
    }
  }

  private val targetAttached = AtomicBoolean(false)

  // TODO [20190112] deal with scaled/transformed View and/or its Parent.
  override val token: ViewToken?
    get() {
      if (target == null || !this.targetAttached.get()) return null

      val playerRect = Rect()
      val visible = target.getGlobalVisibleRect(playerRect, Point())
      if (!visible) return ViewToken(this.priority, playerRect, -1F)

      val drawRect = Rect()
      target.getDrawingRect(drawRect)
      val drawArea = drawRect.width() * drawRect.height()

      var offset = 0f
      if (drawArea > 0) {
        val visibleArea = playerRect.height() * playerRect.width()
        offset = visibleArea / drawArea.toFloat()
      }

      return ViewToken(this.priority, playerRect, offset)
    }

  @CallSuper
  override fun onAdded() {
    super.onAdded()
    target?.run {
      if (ViewCompat.isAttachedToWindow(this)) {
        this@ViewPlayback.onViewAttachedToWindow(this)
      }
      this.addOnAttachStateChangeListener(this@ViewPlayback)
    }
    if (BuildConfig.DEBUG) super.addPlaybackEventListener(this.debugListener)
  }

  override fun onRemoved() {
    if (BuildConfig.DEBUG) super.removePlaybackEventListener(this.debugListener)
    target?.removeOnAttachStateChangeListener(this)
    super.onRemoved()
  }

  override fun onViewAttachedToWindow(v: View) {
    if (this.targetAttached.compareAndSet(false, true)) {
      super.target?.also {
        manager.onTargetActive(it)
        it.addOnLayoutChangeListener(this)
      }
    }
  }

  override fun onViewDetachedFromWindow(v: View) {
    if (this.targetAttached.compareAndSet(true, false)) {
      super.target?.run {
        this.removeOnLayoutChangeListener(this@ViewPlayback)
        manager.onTargetInActive(this)
      }
    }
  }

  override fun unbindInternal() {
    if (this.target != null && this.targetAttached.get()) this.manager.onTargetInActive(target)
  }

  override fun onLayoutChange(
    v: View,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if (target != null && changed(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
      manager.onTargetUpdated(target)
    }
  }

  // Location on screen, with visible offset within target's parent.
  data class ViewToken internal constructor(
    internal val priority: Int,
    internal val viewRect: Rect,
    internal val areaOffset: Float,
    internal val canRelease: Boolean = true
  ) : Token() {
    override fun compareTo(other: Token): Int {
      // TODO [20180813] may need better comparison regarding the orientations.
      return (other as? ViewToken)?.let {
        var result = this.priority.compareTo(other.priority)
        if (result == 0) result = CENTER_Y.compare(this, other)
        if (result == 0) result = this.areaOffset.compareTo(other.areaOffset)
        result
      } ?: super.compareTo(other)
    }

    override fun shouldPrepare(): Boolean {
      return areaOffset >= 0f
    }

    override fun shouldPlay(): Boolean {
      return areaOffset >= 0.65f  // TODO [20180714] make this configurable
    }

    override fun shouldRelease(): Boolean {
      return canRelease
    }

    override fun toString(): String {
      return "$viewRect::$areaOffset"
    }
  }

  @Suppress("unused")
  companion object {
    val CENTER_Y: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.viewRect.centerY(), o2.viewRect.centerY())
    }

    val CENTER_X: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.viewRect.centerX(), o2.viewRect.centerX())
    }

    // Find a CoordinatorLayout parent of View, which doesn't reach 'root' View.
    private fun findSuitableParent(
      root: View,
      target: View?
    ): View? {
      var view = target
      do {
        if (view != null && view.parent is CoordinatorLayout) {
          return view
        } else if (view === root) {
          return null
        }

        if (view != null) {
          // Else, we will loop and crawl up the view hierarchy and try to find a parent
          val parent = view.parent
          view = if (parent is View) parent else null
        }
      } while (view != null)
      return null
    }

    private fun changed(
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      oldLeft: Int,
      oldTop: Int,
      oldRight: Int,
      oldBottom: Int
    ): Boolean {
      return top != oldTop || bottom != oldBottom || left != oldLeft || right != oldRight
    }
  }
}