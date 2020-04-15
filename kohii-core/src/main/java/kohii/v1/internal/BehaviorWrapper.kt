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

package kohii.v1.internal

import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.WindowInsetsCompat
import kohii.v1.core.Manager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
internal class BehaviorWrapper<V : View>(
  internal val delegate: Behavior<in V>,
  manager: Manager
) : Behavior<V>(null, null), Handler.Callback {

  companion object {
    private const val EVENT_IDLE = 1
    private const val EVENT_SCROLL = 2
    private const val EVENT_TOUCH = 3
    private const val EVENT_DELAY = 150L
  }

  private val scrollConsumed = AtomicBoolean(false)
  private val handler = Handler(this)
  private val weakManager = WeakReference(manager)

  override fun handleMessage(msg: Message?): Boolean {
    when (msg?.what) {
      EVENT_SCROLL, EVENT_TOUCH -> {
        scrollConsumed.set(false)
        handler.removeMessages(EVENT_IDLE)
        handler.sendEmptyMessageDelayed(EVENT_IDLE, EVENT_DELAY)
      }
      EVENT_IDLE -> {
        // idle --> consume it.
        if (!scrollConsumed.getAndSet(true)) weakManager.get()?.refresh()
      }
    }
    return true
  }

  internal fun onDetach() {
    handler.removeCallbacksAndMessages(null)
  }

  override fun blocksInteractionBelow(
    parent: CoordinatorLayout,
    child: V
  ): Boolean {
    return delegate.blocksInteractionBelow(parent, child)
  }

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray
  ) {
    delegate.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed)
  }

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {
    delegate.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
  }

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int
  ) {
    delegate.onNestedScroll(
        coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed
    )
  }

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int
  ) {
    delegate.onNestedScroll(
        coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type
    )
  }

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) {
    delegate.onNestedScroll(
        coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type,
        consumed
    )
  }

  override fun onSaveInstanceState(
    parent: CoordinatorLayout,
    child: V
  ): Parcelable? {
    return delegate.onSaveInstanceState(parent, child)
  }

  override fun onNestedScrollAccepted(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int
  ) {
    delegate.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes)
  }

  override fun onNestedScrollAccepted(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ) {
    delegate.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type)
  }

  override fun getScrimColor(
    parent: CoordinatorLayout,
    child: V
  ): Int {
    return delegate.getScrimColor(parent, child)
  }

  override fun onNestedFling(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    velocityX: Float,
    velocityY: Float,
    consumed: Boolean
  ): Boolean {
    return delegate.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
  }

  override fun onLayoutChild(
    parent: CoordinatorLayout,
    child: V,
    layoutDirection: Int
  ): Boolean {
    return delegate.onLayoutChild(parent, child, layoutDirection)
  }

  override fun onNestedPreFling(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    velocityX: Float,
    velocityY: Float
  ): Boolean {
    return delegate.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
  }

  override fun getInsetDodgeRect(
    parent: CoordinatorLayout,
    child: V,
    rect: Rect
  ): Boolean {
    return delegate.getInsetDodgeRect(parent, child, rect)
  }

  override fun onDetachedFromLayoutParams() {
    handler.removeCallbacksAndMessages(null)
    delegate.onDetachedFromLayoutParams()
  }

  override fun onRestoreInstanceState(
    parent: CoordinatorLayout,
    child: V,
    state: Parcelable
  ) {
    delegate.onRestoreInstanceState(parent, child, state)
  }

  override fun onInterceptTouchEvent(
    parent: CoordinatorLayout,
    child: V,
    ev: MotionEvent
  ): Boolean {
    handler.removeCallbacksAndMessages(null)
    handler.sendEmptyMessage(EVENT_TOUCH)
    return delegate.onInterceptTouchEvent(parent, child, ev)
  }

  override fun onDependentViewRemoved(
    parent: CoordinatorLayout,
    child: V,
    dependency: View
  ) {
    delegate.onDependentViewRemoved(parent, child, dependency)
  }

  override fun onStopNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View
  ) {
    delegate.onStopNestedScroll(coordinatorLayout, child, target)
  }

  override fun onStopNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    type: Int
  ) {
    delegate.onStopNestedScroll(coordinatorLayout, child, target, type)
  }

  override fun layoutDependsOn(
    parent: CoordinatorLayout,
    child: V,
    dependency: View
  ): Boolean {
    return delegate.layoutDependsOn(parent, child, dependency)
  }

  override fun onRequestChildRectangleOnScreen(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    rectangle: Rect,
    immediate: Boolean
  ): Boolean {
    return delegate.onRequestChildRectangleOnScreen(coordinatorLayout, child, rectangle, immediate)
  }

  override fun onDependentViewChanged(
    parent: CoordinatorLayout,
    child: V,
    dependency: View
  ): Boolean {
    return delegate.onDependentViewChanged(parent, child, dependency)
  }

  override fun getScrimOpacity(
    parent: CoordinatorLayout,
    child: V
  ): Float {
    return delegate.getScrimOpacity(parent, child)
  }

  override fun onApplyWindowInsets(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    insets: WindowInsetsCompat
  ): WindowInsetsCompat {
    return delegate.onApplyWindowInsets(coordinatorLayout, child, insets)
  }

  override fun onTouchEvent(
    parent: CoordinatorLayout,
    child: V,
    ev: MotionEvent
  ): Boolean {
    handler.removeCallbacksAndMessages(null)
    handler.sendEmptyMessage(EVENT_TOUCH)
    return delegate.onTouchEvent(parent, child, ev)
  }

  override fun onAttachedToLayoutParams(params: LayoutParams) {
    delegate.onAttachedToLayoutParams(params)
  }

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int
  ): Boolean {
    handler.removeCallbacksAndMessages(null)
    handler.sendEmptyMessage(EVENT_SCROLL)
    return delegate.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes)
  }

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    handler.removeCallbacksAndMessages(null)
    handler.sendEmptyMessage(EVENT_SCROLL)
    return delegate.onStartNestedScroll(
        coordinatorLayout, child, directTargetChild, target, axes, type
    )
  }

  override fun onMeasureChild(
    parent: CoordinatorLayout,
    child: V,
    parentWidthMeasureSpec: Int,
    widthUsed: Int,
    parentHeightMeasureSpec: Int,
    heightUsed: Int
  ): Boolean {
    return delegate.onMeasureChild(
        parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed
    )
  }
}
