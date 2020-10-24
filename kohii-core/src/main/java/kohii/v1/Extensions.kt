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

package kohii.v1

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
import androidx.core.util.Pools.Pool
import kohii.v1.internal.BehaviorWrapper
import kotlin.math.abs

/**
 * @author eneim (2018/10/27).
 */
inline fun <T> Pool<T>.onEachAcquired(action: (T) -> Unit) {
  var item: T?
  do {
    item = this.acquire()
    if (item == null) break
    else action(item)
  } while (true)
}

// Return a View that is ancestor of container, and has direct parent is a CoordinatorLayout
internal fun View.findCoordinatorLayoutDirectChildContainer(target: View?): View? {
  // val root = peekDecorView() ?: return null
  var view = target
  do {
    if (view != null && view.parent is CoordinatorLayout) {
      return view
    } else if (view === this) {
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

internal inline fun <T, R> Iterable<T>.partitionToMutableSets(
  predicate: (T) -> Boolean,
  transform: (T) -> R
): Pair<MutableSet<R>, MutableSet<R>> {
  val first = mutableSetOf<R>()
  val second = mutableSetOf<R>()
  for (element in this) {
    if (predicate(element)) {
      first.add(transform(element))
    } else {
      second.add(transform(element))
    }
  }
  return Pair(first, second)
}

/**
 * Formula:
 * > distance = distanceX + distanceY, where distanceX is the diff between the X coordinate of the
 * Rect center and distanceY is the diff between the Y coordinate of the Rect center.
 */
internal infix fun Rect.distanceTo(target: Pair<Float, Float>): Float {
  val distanceX = abs(this.centerX() - target.first)
  val distanceY = abs(this.centerY() - target.second)
  return distanceX + distanceY // no need to be the fancy Euclid sqrt distance.
}

// Learn from Glide: com/bumptech/glide/manager/RequestManagerRetriever.java#L304
internal fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

/** Utility to help client to quickly fetch the original [Behavior] of a View if available */
fun View.viewBehavior(): Behavior<*>? {
  val params = layoutParams
  val behavior = if (params is CoordinatorLayout.LayoutParams) params.behavior else null
  return if (behavior is BehaviorWrapper) behavior.delegate else behavior
}

// Because I want to compose the message first, then log it.
@RestrictTo(LIBRARY_GROUP)
fun String.logDebug(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.d(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP)
fun String.logInfo(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.i(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP)
fun String.logWarn(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.w(tag, this)
  }
}

@RestrictTo(LIBRARY_GROUP)
fun String.logError(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.e(tag, this)
  }
}

internal inline fun debugOnly(action: () -> Unit) {
  if (BuildConfig.DEBUG) action()
}
