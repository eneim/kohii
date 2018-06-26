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
import android.support.annotation.CallSuper
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.view.View
import java.util.Comparator
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
internal class ViewPlayback<V : View>(playable: Playable, uri: Uri, manager: Manager,
    target: V?, options: Playable.Options) : Playback<V>(playable, uri, manager, target,
    options), View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

  private val listener: PlaybackEventListener = object : PlaybackEventListener {
    override fun onBuffering() {
    }

    override fun onPlaying() {
      getTarget()?.keepScreenOn = true
    }

    override fun onPaused() {
    }

    override fun onCompleted() {
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
      return if (offset >= 0.75f)
        ViewToken(playerRect.centerX().toFloat(), playerRect.centerY().toFloat(), offset)
      else
        null
    }

  @CallSuper
  override fun onAdded() {
    super.onAdded()
    val target = getTarget()
    if (target != null) {
      if (ViewCompat.isAttachedToWindow(target)) {
        this.onViewAttachedToWindow(target)
      }
      target.addOnAttachStateChangeListener(this)
    }
  }

  override fun onActive() {
    super.onActive()
    super.addListener(this.listener)
  }

  override fun onInActive() {
    super.onInActive()
    super.removeListener(this.listener)
  }

  override fun onRemoved(recreating: Boolean) {
    super.onRemoved(recreating)
    val target = super.getTarget()
    target?.removeOnAttachStateChangeListener(this)
  }

  override fun onViewAttachedToWindow(v: View) {
    this.targetAttached.set(true)
    val target = super.getTarget()
    if (target != null) {
      // Find a ancestor of target whose parent is a CoordinatorLayout, or null.
      val corChild = findSuitableParent(manager.decorView, target)
      val params = corChild?.layoutParams

      if (params is CoordinatorLayout.LayoutParams) {
        // TODO [20180620] deal with CoordinatorLayout.
      }

      manager.onTargetActive(target)
      target.addOnLayoutChangeListener(this)
    }
  }

  override fun onViewDetachedFromWindow(v: View) {
    this.targetAttached.set(false)
    val target = super.getTarget()
    if (target != null) {
      target.removeOnLayoutChangeListener(this)
      manager.onTargetInActive(target)
    }
  }

  override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
      oldTop: Int, oldRight: Int, oldBottom: Int) {
    if (layoutChanged(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
      manager.onPlaybackInternalChanged(this)
    }
  }

  // Location on screen, with visible offset within target's parent.
  internal class ViewToken internal constructor(internal val centerX: Float,
      internal val centerY: Float, internal val areaOffset: Float) : Token() {
    override fun compareTo(other: Token): Int {
      return if (other is ViewToken) CENTER_Y.compare(this, other) else super.compareTo(other)
    }
  }

  companion object {
    val CENTER_Y: Comparator<ViewToken> = Comparator { o1, o2 ->
      compareValues(o1.centerY, o2.centerY)
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

    private fun layoutChanged(left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int,
        oldTop: Int, oldRight: Int, oldBottom: Int): Boolean {
      return top != oldTop || bottom != oldBottom || left != oldLeft || right != oldRight
    }
  }
}