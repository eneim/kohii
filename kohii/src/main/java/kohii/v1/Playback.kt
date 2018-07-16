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
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Instance of this class will be tight to a Target. And that target is not reusable, so instance
 * of this class must not be passed around out of Activity's scope.
 *
 * @author eneim (2018/06/24).
 */
abstract class Playback<T> internal constructor(
    internal val kohii: Kohii,
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
    private val SCRAP = Any()
  }

  open class Token : Comparable<Token> {
    override fun compareTo(other: Token) = 0

    open fun wantsToPlay(): Boolean {
      return false
    }
  }

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

  // For public access as well.
  val tag: Any

  private val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  private val callbacks = CopyOnWriteArraySet<Callback>()
  private val target: WeakReference<T>?

  internal var internalCallback: InternalCallback? = null
  // Token is comparable.
  // Returning null --> there is nothing to play. A bound Playable should be unbound and released.
  internal open val token: Token?
    get() = null

  @Retention(SOURCE)  //
  @IntDef(STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_END)  //
  annotation class State

  init {
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    this.target = if (target == null) null else WeakReference(target)
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

  internal fun pause() {
    playable.pause()
  }

  @CallSuper
  internal fun play() {
    playable.play()
  }

  @CallSuper
  internal fun release() {
    playable.release()
  }

  // being added to Manager
  // the target may not be attached to View/Window.
  @CallSuper
  open fun onAdded() {
    internalCallback?.onAdded(this)
  }

  // being removed from Manager
  @CallSuper
  open fun onRemoved() {
    internalCallback?.onRemoved(this)
    this.callbacks.clear()
    this.listeners.clear()
  }

  // ~ View is attached
  @CallSuper
  open fun onTargetAvailable() {
    this.callbacks.forEach { it.onTargetAvailable(this) }
  }

  // Playback's onTargetUnAvailable is equal to View's detach event.
  // Once it is inactive, its resource is considered freed and can be cleanup anytime.
  // Proper handling of in-active state must consider to: [1] Save any previous state (PlaybackInfo)
  // into cache, ready to reuse once coming back, [2] consider to release allocated resource if
  // there is no other Manager manages the internal Playable.
  @CallSuper
  open fun onTargetUnAvailable() {
    handler.removeCallbacksAndMessages(null)
    this.callbacks.forEach { it.onTargetUnAvailable(this) }
  }

  @CallSuper
  open fun onCreated() {

  }

  @CallSuper
  open fun onDestroyed() {

  }

  // For internal flow only.
  internal interface InternalCallback {
    fun onAdded(playback: Playback<*>)
    fun onRemoved(playback: Playback<*>)
  }

  interface Callback {
    fun onTargetAvailable(playback: Playback<*>)
    fun onTargetUnAvailable(playback: Playback<*>)
  }
}
