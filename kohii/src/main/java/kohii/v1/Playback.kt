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

import android.net.Uri
import android.os.Handler
import android.support.annotation.CallSuper
import android.support.annotation.IntDef
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.HashSet
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Instance of this class will be tight to a Target. And that target is not reusable, so instance
 * of this class must not be passed around out of Activity's scope.
 *
 * @author eneim (2018/06/24).
 */
abstract class Playback<T> internal constructor(
    internal val playable: Playable,
    internal val uri: Uri,
    internal val manager: Manager,
    target: T?,
    builder: Playable.Builder
    ) {

  companion object {
    const val STATE_IDLE = 1
    const val STATE_BUFFERING = 2
    const val STATE_READY = 3
    const val STATE_END = 4
    val DEFAULT_DISPATCHER = object : Dispatcher {
      override fun delayToStart() = 0 // no delay
    }
    private val SCRAP = Any()
  }

  open class Token : Comparable<Token> {
    override fun compareTo(other: Token) = 0
  }

  @Suppress("unused")
  internal class PlaybackWeakReference<M>(
      val playback: Playback<M>,
      referent: M,
      q: ReferenceQueue<in M>
  ) : WeakReference<M>(referent, q)

  private val handler = Handler(Handler.Callback { msg ->
    val playWhenReady = msg.obj as Boolean
    when (msg.what) {
      STATE_IDLE -> {
      }

      STATE_BUFFERING -> for (listener in listeners) {
        listener.onBuffering()
      }

      STATE_READY -> for (listener in listeners) {
        if (playWhenReady) listener.onPlaying() else listener.onPaused()
      }

      STATE_END -> for (listener in listeners) {
        listener.onCompleted()
      }
    }
    true
  })

  private val listeners = HashSet<PlaybackEventListener>()
  private val callbacks = HashSet<Callback>()

  @Suppress("MemberVisibilityCanBePrivate")
  val target: WeakReference<T>?
  val tag: Any

  // Return null Token will indicate that this Playback cannot start.
  // Token is comparable.
  internal open val token: Token?
    get() = null

  @Retention(SOURCE)  //
  @IntDef(STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_END)  //
  annotation class State

  init {
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    this.target =
        if (target == null)
          null
        else
          PlaybackWeakReference(this, target, manager.kohii.referenceQueue as ReferenceQueue<in T>)
    this.tag = builder.tag ?: SCRAP
  }

  // Used by subclasses to dispatch internal event listeners
  internal fun dispatchPlayerStateChanged(playWhenReady: Boolean, @State playbackState: Int) {
    handler.obtainMessage(playbackState, playWhenReady).sendToTarget()
  }

  fun addListener(listener: PlaybackEventListener) {
    this.listeners.add(listener)
  }

  fun removeListener(listener: PlaybackEventListener) {
    this.listeners.remove(listener)
  }

  fun addCallback(callback: Callback) {
    this.callbacks.add(callback)
  }

  fun removeCallback(callback: Callback?) {
    this.callbacks.remove(callback)
  }

  /// internal APIs

  fun getTarget(): T? {
    return target?.get()
  }

  // Only playback with 'valid tag' will be cached for restoring.
  internal fun validTag() = this.tag !== SCRAP

  internal fun onPause(configChange: Boolean) {
    if (!configChange) {
      if (this.manager.playablesThisActiveTo.contains(playable)) {
        playable.pause()
      }
    }
  }

  internal fun onPlay() {
    playable.play()
  }

  // being added to Manager
  // the target may not be attached to View/Window.
  @CallSuper
  open fun onAdded() {
    this.callbacks.forEach { it.onAdded(this) }
  }

  // being removed from Manager
  @CallSuper
  open fun onRemoved(recreating: Boolean) {
    this.callbacks.forEach { it.onRemoved(this, recreating) }
    this.listeners.clear()
    this.callbacks.clear()
  }

  // ~ View is attached
  @CallSuper
  open fun onActive() {
    this.callbacks.forEach { it.onActive(this) }
  }

  @CallSuper
  open fun onInActive() {
    handler.removeCallbacksAndMessages(null)
    this.callbacks.forEach { it.onInActive(this) }
  }

  interface Callback {

    fun onAdded(playback: Playback<*>)

    fun onActive(playback: Playback<*>)

    fun onInActive(playback: Playback<*>)

    fun onRemoved(playback: Playback<*>, recreating: Boolean)
  }

  interface Dispatcher {

    fun delayToStart(): Int
  }
}
