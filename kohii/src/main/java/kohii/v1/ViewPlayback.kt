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
import android.net.Uri
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
internal class ViewPlayback<V : View>(
    kohii: Kohii,
    playable: Playable,
    uri: Uri,
    manager: Manager,
    target: V?,
    builder: Playable.Builder,
    playbackDispatcher: PlaybackDispatcher
) : Playback<V>(
    kohii, playable, uri, manager, target, builder, playbackDispatcher
), View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

  // For debugging purpose only.
  private val listener = object : PlaybackEventListener {
    override fun onBuffering(playWhenReady: Boolean) {
      Log.d("Kohii:P", "buffering: " + this@ViewPlayback)
    }

    override fun onPlaying() {
      Log.d("Kohii:P", "playing: " + this@ViewPlayback)
      getTarget()?.keepScreenOn = true
    }

    override fun onPaused() {
      Log.w("Kohii:P", "paused: " + this@ViewPlayback)
    }

    override fun onCompleted() {
      Log.d("Kohii:P", "ended: " + this@ViewPlayback)
      getTarget()?.keepScreenOn = false
    }
  }

  private val targetAttached = AtomicBoolean(false)

  override val token: ViewToken?
    get() {
      val target = super.getTarget()
      if (target == null || !this.targetAttached.get()) return null

      val playerRect = Rect()
      val visible = target.getGlobalVisibleRect(playerRect, Point())
      if (!visible) return null

      val drawRect = Rect()
      target.getDrawingRect(drawRect)
      val drawArea = drawRect.width() * drawRect.height()

      var offset = 0f
      if (drawArea > 0) {
        val visibleArea = playerRect.height() * playerRect.width()
        offset = visibleArea / drawArea.toFloat()
      }
      return ViewToken(playerRect.centerX().toFloat(), playerRect.centerY().toFloat(), offset)
    }

  @CallSuper
  override fun onAdded() {
    super.getTarget()?.run {
      if (ViewCompat.isAttachedToWindow(this)) {
        this@ViewPlayback.onViewAttachedToWindow(this)
      }
      this.addOnAttachStateChangeListener(this@ViewPlayback)
    }
    super.onAdded()
  }

  override fun onTargetAvailable() {
    super.addPlaybackEventListener(this.listener)
    super.onTargetAvailable()
  }

  override fun onTargetUnAvailable() {
    super.onTargetUnAvailable()
    super.removePlaybackEventListener(this.listener)
  }

  override fun onRemoved() {
    super.onRemoved()
    super.getTarget()?.removeOnAttachStateChangeListener(this)
  }

  override fun onViewAttachedToWindow(v: View) {
    if (this.targetAttached.compareAndSet(false, true)) {
      val target = super.getTarget()
      if (target != null) {
        // Find a ancestor of target whose parent is a CoordinatorLayout, or null.
        val corChild = findSuitableParent(manager.decorView, target)
        val params = corChild?.layoutParams
        if (params is CoordinatorLayout.LayoutParams) {
          // TODO [20180620] deal with CoordinatorLayout.
        }

        manager.onTargetAvailable(target)
        target.addOnLayoutChangeListener(this)
      }
    }
  }

  override fun onViewDetachedFromWindow(v: View) {
    if (this.targetAttached.compareAndSet(true, false)) {
      super.getTarget()?.run {
        this.removeOnLayoutChangeListener(this@ViewPlayback)
        manager.onTargetUnAvailable(this)
      }
    }
  }

  override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
      oldTop: Int, oldRight: Int, oldBottom: Int) {
    if (layoutDidChange(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
      manager.onPlaybackInternalChanged(this)
    }
  }

  // Location on screen, with visible offset within target's parent.
  @Suppress("MemberVisibilityCanBePrivate")
  internal data class ViewToken internal constructor(
      internal val centerX: Float,
      internal val centerY: Float,
      internal val areaOffset: Float
  ) : Token() {
    override fun compareTo(other: Token): Int {
      return if (other is ViewToken) CENTER_Y.compare(this, other) else super.compareTo(other)
    }

    override fun wantsToPlay(): Boolean {
      return areaOffset >= 0.75f  // TODO [20180714] make this configurable
    }
  }

  @Suppress("unused")
  companion object {
    val CENTER_Y: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.centerY, o2.centerY)
    }

    val CENTER_X: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.centerX, o2.centerX)
    }

    // Find a CoordinatorLayout parent of View, which doesn't reach 'root' View.
    private fun findSuitableParent(root: View, target: View?): View? {
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

    private fun layoutDidChange(left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
        oldTop: Int, oldRight: Int, oldBottom: Int): Boolean {
      return top != oldTop || bottom != oldBottom || left != oldLeft || right != oldRight
    }
  }
}