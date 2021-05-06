/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v2.core

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import kohii.v2.internal.checkMainThread
import kohii.v2.internal.onNotNull
import java.util.ArrayDeque

/**
 * A class that represents a [Fragment] or an [Activity], and has the capability to manage multiple
 * [Playback]s.
 */
class Manager {

  @VisibleForTesting
  internal val buckets = ArrayDeque<Bucket>(4 /* Avoid size check */)

  @VisibleForTesting
  internal var stickyBucket: Bucket? = null

  private val playbacks = linkedMapOf<Any /* Container type */, Playback>()

  internal fun refresh() {
    // TODO(eneim): implement this.
  }

  //region Bucket APIs
  @MainThread
  internal fun addBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (!buckets.contains(bucket)) {
      buckets.add(bucket)
      bucket.onAdd()
      refresh()
    }
  }

  @MainThread
  internal fun removeBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (buckets.remove(bucket)) {
      bucket.onRemove()
      refresh()
    }
  }

  @MainThread
  internal fun stickBucketInternal(bucket: Bucket) {
    checkMainThread()
    require(buckets.contains(bucket)) {
      "$bucket is not registered. Please add it before stick it."
    }
    if (bucket !== stickyBucket) {
      stickyBucket.onNotNull(::unstickBucketInternal)
      stickyBucket = bucket
      buckets.push(bucket)
      refresh()
    }
  }

  @MainThread
  internal fun unstickBucketInternal(bucket: Bucket) {
    checkMainThread()
    if (stickyBucket === bucket && bucket === buckets.peekFirst()) {
      buckets.removeFirst()
      stickyBucket = null
      refresh()
    }
  }
  //endregion

  //region Playback APIs
  @Suppress("unused")
  @MainThread
  internal fun addPlayback(playback: Playback) {
    checkMainThread()
    val container = playback.container
    val removedPlayback: Playback? = playbacks.put(container, playback)
    // No existing Playback are allowed.
    require(removedPlayback == null) {
      "Adding $playback for container $container, but found existing one: $removedPlayback."
    }
    // Playback should not let the bucket touch its container at this point.
    playback.performAdd()
    playback.bucket.addContainer(container)
    refresh()
  }

  @MainThread
  internal fun removePlayback(playback: Playback) {
    checkMainThread()
    val container = playback.container
    val removedPlayback = playbacks.remove(container)
    require(removedPlayback === playback) {
      "Removing $playback for container $container, but got a different one $removedPlayback"
    }
    if (playback.isStarted) {
      if (playback.isResumed) playback.performPause()
      playback.performStop()
    }
    playback.bucket.removeContainer(container)
    // Playback should not let the bucket touch its container at this point.
    playback.performRemove()
    refresh()
  }
  //endregion

  //region Container APIs
  @MainThread
  internal fun onContainerStarted(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    checkNotNull(playback) {
      "Container $container is managed, but no corresponding playback are found."
    }
    check(playback.isAdded) { "Playback $playback is not added." }
    playback.performStart()
    playback.performResume() // TODO(eneim): check if we can resume immediately?
    refresh()
  }

  @MainThread
  internal fun onContainerStopped(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    checkNotNull(playback) {
      "Container $container is managed, but no corresponding playback are found."
    }
    if (playback.isStarted) {
      if (playback.isResumed) playback.performPause()
      playback.performStop()
    }
    refresh()
  }

  @MainThread
  internal fun onContainerUpdated(container: Any) {
    checkMainThread()
    val playback = playbacks[container]
    check(playback != null) {
      "Container $container is managed, but no corresponding playback are found."
    }
    refresh()
  }

  // Note(eneim, 2021/04/30): removing a Playback may trigger Bucket.removeContainer, which calls
  // this method again. So we allow the cached Playback to be null.
  @MainThread
  internal fun onContainerRemoved(container: Any) {
    checkMainThread()
    playbacks[container]?.let(::removePlayback)
  }
  //endregion
}
