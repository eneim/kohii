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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.Pools.Pool
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.AudioComponent
import kohii.media.VolumeInfo
import kotlin.math.abs

/**
 * @author eneim (2018/10/27).
 */
fun Player.getVolumeInfo(): VolumeInfo {
  return when (this) {
    is VolumeInfoController -> VolumeInfo(this.volumeInfo)
    is AudioComponent -> { // will match SimpleExoPlayer
      val volume = this.volume
      VolumeInfo(volume == 0f, volume)
    }
    else -> throw UnsupportedOperationException(javaClass.name + " doesn't support this.")
  }
}

fun Player.setVolumeInfo(volume: VolumeInfo) {
  when (this) {
    is VolumeInfoController -> this.setVolumeInfo(volume)
    is AudioComponent -> { // will match SimpleExoPlayer
      if (volume.mute) {
        this.volume = 0f
      } else {
        this.volume = volume.volume
      }
      this.setAudioAttributes(this.audioAttributes, !volume.mute)
    }
    else -> throw UnsupportedOperationException(javaClass.name + " doesn't support this.")
  }
}

fun Player.addEventListener(listener: PlayerEventListener) {
  this.addListener(listener)
  this.videoComponent?.addVideoListener(listener)
  this.audioComponent?.addAudioListener(listener)
  this.textComponent?.addTextOutput(listener)
  this.metadataComponent?.addMetadataOutput(listener)
}

fun Player.removeEventListener(listener: PlayerEventListener?) {
  this.removeListener(listener)
  this.videoComponent?.removeVideoListener(listener)
  this.audioComponent?.removeAudioListener(listener)
  this.textComponent?.removeTextOutput(listener)
  this.metadataComponent?.removeMetadataOutput(listener)
}

inline fun <T> Pool<T>.onEachAcquired(action: (T) -> Unit) {
  var item: T?
  do {
    item = this.acquire()
    if (item == null) break
    else action(item)
  } while (true)
}

// Return a View that is ancestor of container, and has direct parent is a CoordinatorLayout
fun findCoordinatorLayoutDirectChildContainer(
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

internal infix fun Rect.distanceTo(target: Pair<Pair<Int, Int>, Pair<Int, Int>>): Int {
  val (targetCenterX, targetHalfWidth) = target.first
  val (targetCenterY, targetHalfHeight) = target.second
  val distanceX = abs(this.centerX() - targetCenterX) / targetHalfWidth
  val distanceY = abs(this.centerY() - targetCenterY) / targetHalfHeight
  return distanceX + distanceY // no need to be the fancy Euclid sqrt distance.
}

// Learn from Glide: com/bumptech/glide/manager/RequestManagerRetriever.java#L304
fun Context.findActivity(): Activity? {
  return if (this is Activity) this else if (this is ContextWrapper) baseContext.findActivity() else null
}

// Because I want to compose the message first, then log it.
internal fun String.logDebug(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.d(tag, this)
  }
}

internal fun String.logInfo(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.i(tag, this)
  }
}

internal fun String.logWarn(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.w(tag, this)
  }
}

internal fun String.logError(tag: String = "${BuildConfig.LIBRARY_PACKAGE_NAME}.log") {
  if (BuildConfig.DEBUG) {
    Log.e(tag, this)
  }
}
