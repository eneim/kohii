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

import android.os.Handler
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.VolumeInfo
import kohii.v1.Playable.Companion.STATE_BUFFERING
import kohii.v1.Playable.Companion.STATE_END
import kohii.v1.Playable.Companion.STATE_IDLE
import kohii.v1.Playable.Companion.STATE_READY
import kohii.v1.Playable.State
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Instance of this class will be tight to a Target. And that target is not reusable, so instance
 * of this class must not be passed around out of Activity's scope.
 *
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Playback<T> internal constructor(
  internal val kohii: Kohii,
  internal val playable: Playable<T>,
  internal val manager: PlaybackManager,
  internal val target: T?,
  @Priority
  internal val priority: Int = PRIORITY_NORMAL,
  internal val delay: () -> Long = NO_DELAY // Being a function so its result is dynamic.
) : LifecycleObserver {

  companion object {
    const val DELAY_INFINITE = -1L
    val NO_DELAY = { 0L }

    // Priority
    const val PRIORITY_HIGH = 1
    const val PRIORITY_NORMAL = 2
    const val PRIORITY_LOW = 3
  }

  @Retention(SOURCE)
  @IntDef(PRIORITY_HIGH, PRIORITY_NORMAL, PRIORITY_LOW)
  annotation class Priority

  open class Token : Comparable<Token> {
    override fun compareTo(other: Token) = 0

    open fun shouldPlay() = false

    // Called by Manager, to know if a Playback should start preparing or not. True by default.
    open fun shouldPrepare() = true

    // Called by Manager, to know if a Playback should release or not. True by default.
    open fun shouldRelease() = true
  }

  private var listenerHandler: Handler? = null

  // Listeners for Playable. Playable will access these filed on demand.
  internal val volumeListeners by lazy { VolumeChangedListeners() }
  internal val playerListeners by lazy { PlayerEventListeners() }
  internal val errorListeners by lazy { ErrorListeners() }
  internal var internalCallback: InternalCallback? = null

  // Internal callbacks
  internal val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  internal val callbacks = CopyOnWriteArraySet<Callback>()

  // Token is comparable.
  // Returning null --> there is nothing to play. A bound Playable should be unbound.
  internal open val token: Token?
    get() = null

  // [BEGIN] Public API

  fun addPlaybackEventListener(listener: PlaybackEventListener) {
    this.listeners.add(listener)
  }

  fun removePlaybackEventListener(listener: PlaybackEventListener?) {
    this.listeners.remove(listener)
  }

  fun addCallback(callback: Callback) {
    this.callbacks.add(callback)
  }

  fun removeCallback(callback: Callback?) {
    this.callbacks.remove(callback)
  }

  fun addVolumeChangeListener(listener: VolumeChangedListener) {
    this.volumeListeners.add(listener)
  }

  fun removeVolumeChangeListener(listener: VolumeChangedListener?) {
    this.volumeListeners.remove(listener)
  }

  fun addPlayerEventListener(listener: PlayerEventListener) {
    this.playerListeners.add(listener)
  }

  fun removePlayerEventListener(listener: PlayerEventListener?) {
    this.playerListeners.remove(listener)
  }

  var volumeInfo: VolumeInfo
    get() {
      return playable.volumeInfo
    }
    set(value) {
      playable.setVolumeInfo(value)
    }

  val tag = playable.tag

  fun unbind() {
    this.unbindInternal()
    manager.performRemovePlayback(this)
  }

  // [END] Public API

  /// Internal APIs

  // Lifecycle

  internal open fun unbindInternal() {
    if (this.target != null) manager.onTargetInActive(this.target)
  }

  // Used by subclasses to dispatch internal event listeners
  internal fun dispatchPlayerStateChanged(playWhenReady: Boolean, @State playbackState: Int) {
    listenerHandler?.obtainMessage(playbackState, playWhenReady)
        ?.sendToTarget()
  }

  internal fun dispatchFirstFrameRendered() {
    listeners.forEach { it.onFirstFrameRendered() }
  }

  internal fun prepare() {
    playable.prepare()
  }

  internal fun play() {
    listeners.forEach { it.beforePlay() }
    playable.play()
  }

  internal fun pause() {
    playable.pause()
    listeners.forEach { it.afterPause() }
  }

  internal fun release() {
    playable.release()
  }

  internal fun observe(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(this)
  }

  @CallSuper
  internal open fun onCreated() {
    this.listenerHandler = Handler(Handler.Callback { message ->
      val playWhenReady = message.obj as Boolean
      when (message.what) {
        STATE_IDLE -> {
        }
        STATE_BUFFERING -> listeners.forEach { it.onBuffering(playWhenReady) }
        STATE_READY -> listeners.forEach { if (playWhenReady) it.onPlaying() else it.onPaused() }
        STATE_END -> listeners.forEach { it.onCompleted() }
      }
      true
    })
    this.playable.onPlaybackCreated(this)
  }

  // Being added to Manager
  // The target may not be attached to View/Window yet.
  @CallSuper
  internal open fun onAdded() {
    internalCallback?.onAdded(this)
  }

  // ~ View is attached
  @CallSuper
  internal open fun onActive() {
    this.callbacks.forEach {
      it.onActive(this, this.target)
    }
  }

  // Playback's onInActive is equal to View's detach event or Activity stops.
  // Once it is inactive, its resource is considered freed and can be cleanup anytime.
  // Proper handling of in-active state must consider to: [1] Save any previous state (PlaybackInfo)
  // to cache, ready to reuse once coming back, [2] Consider to release allocated resource if there
  // is no other Manager manages the internal Playable.
  @CallSuper
  internal open fun onInActive() {
    this.callbacks.forEach {
      it.onInActive(this, this.target)
    }
  }

  // Being removed from Manager
  @CallSuper
  internal open fun onRemoved() {
    internalCallback?.onRemoved(this)
    listenerHandler?.removeCallbacksAndMessages(null)
    this.callbacks.clear()
    this.listeners.clear()
  }

  @CallSuper
  internal open fun onDestroyed() {
    this.listenerHandler = null
    this.playable.onPlaybackDestroyed(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onLifecycleDestroyed(lifecycleOwner: LifecycleOwner) {
    (this.target as? Any)?.let { manager.onTargetInActive(it) }
    manager.performRemovePlayback(this)
    lifecycleOwner.lifecycle.removeObserver(this)
  }

  override fun toString(): String {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    return "$firstPart::$$playable"
  }

  // For internal flow only.
  internal interface InternalCallback {
    // Called when the playback is added to Manager
    fun onAdded(playback: Playback<*>) {}

    // Called when the playback is removed from Manager
    fun onRemoved(playback: Playback<*>) {}
  }

  interface Callback {
    fun onActive(
      playback: Playback<*>,
      target: Any?
    ) {
    }

    fun onInActive(
      playback: Playback<*>,
      target: Any?
    ) {
    }
  }
}
