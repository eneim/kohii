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

import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.core.Playback.State.RESUMED
import kohii.v2.core.Playback.State.STARTED
import kohii.v2.internal.checkMainThread
import java.util.ArrayDeque

/**
 * An object that contains the information about the surface to play the media content.
 */
abstract class Playback(
  val bucket: Bucket,
  val container: Any,
) {

  // Note(eneim, 2021/04/30): Using ArrayDeque because it is fast and light-weight. It supports
  // iterating in both direction, which is nice. All the access to the callbacks are on the main
  // thread, so we do not need thread-safety. We do not need the ability to modify the callbacks
  // during iteration as well. While ArrayDeque is well-known as the best queue implementation, we
  // do not use it as a queue. But it is still a good choice for our use case.
  private val callbacks = ArrayDeque<Callback>(4 /* Skip internal size check */)

  /**
   * Returns the current state of the Playback.
   */
  internal var state: State = CREATED
    @VisibleForTesting internal set

  val isAdded: Boolean get() = state >= ADDED
  val isStarted: Boolean get() = state >= STARTED
  val isResumed: Boolean get() = state >= RESUMED

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

  /**
   * Called by the [Manager] to perform adding this Playback. This method will call [onAdd], and its
   * state will be changed: [CREATED]->[onAdd]->[ADDED].
   */
  @MainThread
  internal fun performAdd() {
    checkMainThread()
    checkState(CREATED)
    onAdd()
    state = ADDED
    for (callback in callbacks) {
      callback.onAdded(this)
    }
  }

  /**
   * Called by the [Manager] to perform starting this Playback.
   *
   * This method will call [onStart], its state will be changed: [ADDED]->[onStart]->[STARTED].
   */
  @MainThread
  internal fun performStart() {
    checkMainThread()
    checkState(ADDED)
    onStart()
    state = STARTED
    for (callback in callbacks) {
      callback.onStarted(this)
    }
  }

  /**
   * Called by the [Manager] to perform resuming this Playback.
   *
   * This method will call [onResume], its state will be changed: [STARTED]->[onResume]->[RESUMED].
   */
  @MainThread
  internal fun performResume() {
    checkMainThread()
    checkState(STARTED)
    onResume()
    state = RESUMED
    for (callback in callbacks) {
      callback.onResumed(this)
    }
  }

  /**
   * Called by the [Manager] to perform pausing this Playback.
   *
   * This method will call [onPause], its state will be changed: [RESUMED]->[STARTED]->[onPause].
   */
  @MainThread
  internal fun performPause() {
    checkMainThread()
    checkState(RESUMED)
    state = STARTED
    for (callback in callbacks) {
      callback.onPaused(this)
    }
    onPause()
  }

  /**
   * Called by the [Manager] to perform stopping this Playback.
   *
   * This method will call [onStop], its state will be changed: [STARTED]->[ADDED]->[onStop].
   */
  @MainThread
  internal fun performStop() {
    checkMainThread()
    checkState(STARTED)
    state = ADDED
    for (callback in callbacks) {
      callback.onStopped(this)
    }
    onStop()
  }

  /**
   * Called by the [Manager] to perform removing this Playback.
   *
   * This method will call [onRemove], its state will be changed: [ADDED]->[REMOVED]->[onRemove].
   */
  @MainThread
  internal fun performRemove() {
    checkMainThread()
    checkState(ADDED)
    state = REMOVED
    for (callback in callbacks) {
      callback.onRemoved(this)
    }
    onRemove()
  }

  @MainThread
  @CallSuper
  protected open fun onAdd(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onRemove(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onStart(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onStop(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onResume(): Unit = Unit

  @MainThread
  @CallSuper
  protected open fun onPause(): Unit = Unit

  /**
   * See also [androidx.lifecycle.Lifecycle.State]
   */
  enum class State {
    /**
     * Removed state for a Playback. After this state is reached, the Playback is no longer usable.
     * This state is reached right before a call to [onRemove].
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

    /**
     * Called when the [playback] is added to the manager. [Playback.state] is [ADDED].
     */
    @JvmDefault
    @MainThread
    fun onAdded(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is remove from the manager. [Playback.state] is [REMOVED].
     */
    @JvmDefault
    @MainThread
    fun onRemoved(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is started. If the [Playback.container] is a [View], this event is
     * the same as the [View.onAttachedToWindow] event. [Playback.state] is [STARTED].
     */
    @JvmDefault
    @MainThread
    fun onStarted(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is stopped. If the [Playback.container] is a [View], this event is
     * the same as the [View.onDetachedFromWindow] event. [Playback.state] is [ADDED].
     */
    @JvmDefault
    @MainThread
    fun onStopped(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is resumed. If the [Playback.container] is a [View], this event is
     * the same as when the container has at least one pixel on the screen.
     */
    @JvmDefault
    @MainThread
    fun onResumed(playback: Playback): Unit = Unit

    /**
     * Called when the [playback] is paused. If the [Playback.container] is a [View], this event is
     * the same as when the container doesn't meed the "resume" condition.
     */
    @JvmDefault
    @MainThread
    fun onPaused(playback: Playback): Unit = Unit
  }

  companion object {

    @Throws(IllegalStateException::class)
    private fun Playback.checkState(expected: State): Unit =
      check(state == expected) { "Expected Playback state: $expected, Actual state: $state" }
  }
}
