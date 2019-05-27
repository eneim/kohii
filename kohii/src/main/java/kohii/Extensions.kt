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

package kohii

import android.view.View
import androidx.collection.SparseArrayCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.util.Pools.Pool
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.AudioComponent
import kohii.media.VolumeInfo
import kohii.v1.PlayerEventListener
import kohii.v1.VolumeInfoController

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

// Apply a transformer on each item, return the first result that suffices the predicate.
inline fun <T, R> Iterable<T>.takeFirstOrNull(
  transformer: (T) -> R,
  predicate: (R) -> Boolean = { it != null } // default predicate
): R? {
  for (element in this) {
    val result = transformer.invoke(element)
    if (predicate(result)) return result
  }
  return null
}

// Find a CoordinatorLayout parent
@Suppress("unused")
fun findSuitableParent(
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

inline fun <E> SparseArrayCompat<E>.getOrPut(
  key: Int,
  creator: () -> E
): E {
  var result = this[key]
  if (result == null) {
    result = creator.invoke()
    this.put(key, result)
  }
  return result!!
}

inline fun <E> SparseArrayCompat<E>.forEach(actor: (E, Int) -> Unit) {
  val size = this.size()
  if (size > 0) {
    for (index in 0 until size) {
      val key = this.keyAt(index)
      val value = this.valueAt(index)
      actor.invoke(value, key)
    }
  }
}

fun <T> Set<T>.plusNotNull(element: T?): Set<T> {
  if (element != null) return this + element
  return this
}
