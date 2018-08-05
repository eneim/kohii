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
    internal val playable: Playable,
    internal val uri: Uri,
    internal val manager: Manager,
    internal val target: T?,
    internal val builder: Playable.Builder,
    internal val dispatcher: Delayer = NO_DELAY
) {

  companion object {
    const val STATE_IDLE = 1
    const val STATE_BUFFERING = 2
    const val STATE_READY = 3
    const val STATE_END = 4
    val NO_DELAY = object : Delayer {
      override fun getInitDelay() = 0L
    }
    private const val MSG_PLAY = 100
    private val SCRAP = Any()
  }

  data class Builder(
      internal val kohii: Kohii,
      internal val playable: Playable,
      internal val uri: Uri,
      internal val manager: Manager,
      internal val dispatcher: Delayer,
      internal val builder: Playable.Builder
  )

  open class Token : Comparable<Token> {
    override fun compareTo(other: Token) = 0

    open fun wantsToPlay(): Boolean {
      return false
    }
  }

  private var listenerHandler: Handler? = null
  private var dispatcherHandler: Handler? = null

  // For public access as well.
  val tag: Any

  // listener for Playable
  internal val volumeListeners by lazy { VolumeChangedListeners() }
  internal val playerListeners by lazy { PlayerEventListeners() }
  internal val errorListeners by lazy { ErrorListeners() }

  // internal callbacks
  private val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  private val callbacks = CopyOnWriteArraySet<Callback>()

  internal var internalCallback: InternalCallback? = null
  // Token is comparable.
  // Returning null --> there is nothing to play. A bound Playable should be unbound and released.
  internal open val token: Token?
    get() = null

  @Retention(SOURCE)  //
  @IntDef(STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_END)  //
  annotation class State

  init {
    this.tag = builder.tag ?: SCRAP
  }

  // Used by subclasses to dispatch internal event listeners
  internal fun dispatchPlayerStateChanged(playWhenReady: Boolean, @State playbackState: Int) {
    listenerHandler?.obtainMessage(playbackState, playWhenReady)?.sendToTarget()
  }

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

  /// internal APIs

  // Only playback with 'valid tag' will be cached for restoring.
  internal fun validTag() = this.tag !== SCRAP

  internal fun prepare() {
    playable.prepare()
  }

  internal fun play() {
    val delay = dispatcher.getInitDelay()
    dispatcherHandler?.removeMessages(MSG_PLAY)
    when {
      delay > 0 -> dispatcherHandler?.sendEmptyMessageDelayed(MSG_PLAY, delay) ?: playable.play()
      delay == 0L -> playable.play()
      delay < 0 -> {
        // Do nothing.
      }
    }
  }

  internal fun pause() {
    dispatcherHandler?.removeMessages(MSG_PLAY)
    playable.pause()
  }

  internal fun release() {
    dispatcherHandler?.removeMessages(MSG_PLAY)
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
    dispatcherHandler?.removeCallbacksAndMessages(null)
    listenerHandler?.removeCallbacksAndMessages(null)
    internalCallback?.onRemoved(this)
    this.callbacks.clear()
    this.listeners.clear()
  }

  // ~ View is attached
  @CallSuper
  open fun onTargetAvailable() {
    this.callbacks.forEach { it.onTargetAvailable(this) }
  }

  // Playback's onTargetUnAvailable is equal to View's detach event or Activity stops.
  // Once it is inactive, its resource is considered freed and can be cleanup anytime.
  // Proper handling of in-active state must consider to: [1] Save any previous state (PlaybackInfo)
  // into cache, ready to reuse once coming back, [2] consider to release allocated resource if
  // there is no other Manager manages the internal Playable.
  @CallSuper
  open fun onTargetUnAvailable() {
    this.callbacks.forEach { it.onTargetUnAvailable(this) }
  }

  @CallSuper
  open fun onCreated() {
    this.listenerHandler = Handler(android.os.Handler.Callback {
      val playWhenReady = it.obj as Boolean
      when (it.what) {
        STATE_IDLE -> {
        }

        STATE_BUFFERING -> for (listener in listeners) {
          listener.onBuffering(playWhenReady)
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

    this.dispatcherHandler = Handler(Handler.Callback {
      if (it.what == MSG_PLAY) {
        playable.play()
      }
      true
    })
  }

  @CallSuper
  open fun onDestroyed() {
    this.listenerHandler = null
    this.dispatcherHandler = null
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

  interface Delayer {
    // return the delay to start the Playback.
    // default implementation will use Handler to dispatch the call to "play()", accept that:
    // - if returns negative, it will postpone the playback.
    // - if returns zero, it will start the playback immediately without using Handler.
    fun getInitDelay(): Long
  }
}
