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

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.core.Playback.State.RESUMED
import kohii.v2.core.Playback.State.STARTED
import kohii.v2.internal.InternalUtils.checkMainThread
import java.util.ArrayDeque

/**
 * An object that contains the information about the surface to play the media content.
 */
abstract class Playback {

  // Note(eneim, 2021/04/30): Using ArrayDeque because it is fast and light-weight. It supports
  // iterating in both direction, which is nice. All the access to the callbacks are on the main
  // thread, so we do not need thread-safety. We do not need the ability to modify the callbacks
  // during iteration as well. While ArrayDeque is well-known as the best queue implementation, we
  // do not use it as a queue. But it is still a good choice for our use case.
  private val callbacks = ArrayDeque<Callback>()

  /**
   * Returns the current state of the Playback.
   */
  var state: State = CREATED
    @VisibleForTesting internal set

  @MainThread
  internal fun addCallback(callback: Callback) {
    checkMainThread()
    callbacks.add(callback)
  }

  @Suppress("unused")
  @MainThread
  internal fun removeCallback(callback: Callback?) {
    checkMainThread()
    callbacks.remove(callback)
  }

  @MainThread
  internal fun onAdd() {
    checkMainThread()
    checkState(CREATED)
    state = ADDED
    for (callback in callbacks) {
      callback.onAdded(this)
    }
  }

  @MainThread
  internal fun onRemove() {
    checkMainThread()
    checkState(ADDED)
    state = REMOVED
    for (callback in callbacks) {
      callback.onRemoved(this)
    }
  }

  @MainThread
  internal fun onStart() {
    checkMainThread()
    checkState(ADDED)
    state = STARTED
    for (callback in callbacks) {
      callback.onStarted(this)
    }
  }

  @MainThread
  internal fun onStop() {
    checkMainThread()
    checkState(STARTED)
    state = ADDED
    for (callback in callbacks) {
      callback.onStopped(this)
    }
  }

  @MainThread
  internal fun onResume() {
    checkMainThread()
    checkState(STARTED)
    state = RESUMED
    for (callback in callbacks) {
      callback.onResumed(this)
    }
  }

  @MainThread
  internal fun onPause() {
    checkMainThread()
    checkState(RESUMED)
    state = STARTED
    for (callback in callbacks) {
      callback.onPaused(this)
    }
  }

  /**
   * See also [androidx.lifecycle.Lifecycle.State]
   */
  enum class State {
    /**
     * Removed state for a Playback. After this state is reached, the Playback is no longer usable.
     * This state is reached after a call to [onRemove].
     */
    REMOVED,

    /**
     * Created state for a Playback. This is the state when a Playback is initialized but is not
     * added to its Manager.
     */
    CREATED,

    /**
     * Added state for a Playback. This is the state when a Playback is added to its Manager. This
     * state is reached in two cases:
     * - Right after a call to [onAdd].
     * - Right before a call to [onStop].
     */
    ADDED,

    /**
     * Started state for a Playback. This is the state when the container of a Playback is attached
     * to its parent. This state is reached in two cases:
     * - Right after a call to [onStart].
     * - Right before a call to [onPause].
     */
    STARTED,

    /**
     * Resumed state for a Playback. This is the state when the container of a Playback is attached,
     * and visible enough so that it can be playing at any time. This state is reached right after
     * [onResume] is called.
     */
    RESUMED,
  }

  /**
   * Class that can receive the changes of a Playback lifecycle via callback methods.
   */
  interface Callback {

    @JvmDefault
    @MainThread
    fun onAdded(playback: Playback) = Unit

    @JvmDefault
    @MainThread
    fun onRemoved(playback: Playback) = Unit

    @JvmDefault
    @MainThread
    fun onStarted(playback: Playback) = Unit

    @JvmDefault
    @MainThread
    fun onStopped(playback: Playback) = Unit

    @JvmDefault
    @MainThread
    fun onResumed(playback: Playback) = Unit

    @JvmDefault
    @MainThread
    fun onPaused(playback: Playback) = Unit
  }

  companion object {

    @Throws(IllegalStateException::class)
    private fun Playback.checkState(expected: State): Unit =
      check(state == expected) { "Expected Playback state: $expected, Actual state: $state" }

    private inline fun Playback.whenCreated(block: () -> Unit) {
      if (state != REMOVED) block()
    }
  }
}
