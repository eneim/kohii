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
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import kohii.media.Media
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
abstract class Playback<TARGET, PLAYER> internal constructor(
  internal val kohii: Kohii,
  internal val media: Media,
  internal val playable: Playable<PLAYER>,
  internal val manager: PlaybackManager,
  internal var container: Container, // Manager will update this on demand.
  val target: TARGET,
  internal val config: Playback.Config
) {

  companion object {
    const val TAG = "Kohii::PB"
    const val DELAY_INFINITE = -1L

    // Priority
    const val PRIORITY_HIGH = 1
    const val PRIORITY_NORMAL = 2
    const val PRIORITY_LOW = 3

    val VERTICAL_COMPARATOR = Comparator<Playback<*, *>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, Container.VERTICAL)
    }

    val HORIZONTAL_COMPARATOR = Comparator<Playback<*, *>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, Container.HORIZONTAL)
    }

    val BOTH_AXIS_COMPARATOR = Comparator<Playback<*, *>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, Container.BOTH_AXIS)
    }
  }

  @Retention(SOURCE)
  @IntDef(PRIORITY_HIGH, PRIORITY_NORMAL, PRIORITY_LOW)
  annotation class Priority

  open class Token : Comparable<Token> {
    override fun compareTo(other: Token) = 0

    // = wantsToPlay()
    open fun shouldPlay() = false

    // Called by Manager, to know if a Playback should start preparing or not. True by default.
    // = allowsToPlay(player)
    open fun shouldPrepare() = false
  }

  class Config(
    @Priority
    val priority: Int = PRIORITY_NORMAL,
    val delay: Int = 0,
      // Indicator to used to judge of a Playback should be played or not.
      // This doesn't warranty that it will be played, it just to make the Playback be a candidate
      // to start a playback.
      // In ViewPlayback, this is equal to visible area offset of the video container View.
    val threshold: Float = 0.65F
  )

  // Listeners for Playable. Playable will access these filed on demand.
  internal val volumeListeners by lazy { VolumeChangedListeners() }
  internal val playerListeners by lazy { PlayerEventListeners() }
  internal val errorListeners by lazy { ErrorListeners() }
  internal var playerCallback: PlayerCallback<PLAYER>? = null

  private var listenerHandler: Handler? = null

  // Internal callbacks
  private val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  private val callbacks = CopyOnWriteArraySet<Callback>()

  // Token is comparable.
  // Returning null --> there is nothing to play. A bound Playable should be unbound.
  internal abstract val token: Token

  internal abstract val playerView: PLAYER?

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

  // Internal APIs

  // Lifecycle

  open fun compareWidth(
    other: Playback<*, *>,
    orientation: Int
  ): Int {
    return this.token.compareTo(other.token)
  }

  internal open fun unbindInternal() {
    manager.onTargetInActive(this.target)
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

  internal open fun play() {
    listeners.forEach { it.beforePlay() }
    playable.play()
  }

  internal open fun pause() {
    playable.pause()
    listeners.forEach { it.afterPause() }
  }

  internal fun release() {
    Log.i("Kohii::X", "release: $this, $manager")
    playable.release()
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
    this.addCallback(this.playable)
  }

  // Being added to Manager
  // The target may not be attached to View/Window yet.
  @CallSuper
  internal open fun onAdded() {
    for (callback in this.callbacks) {
      callback.onAdded(this)
    }
    container.attachTarget(target)
  }

  // ~ View is attached
  @CallSuper
  internal open fun onActive() {
    for (callback in this.callbacks) {
      callback.onActive(this)
    }
    val player = this.playerView
    if (player != null && player === this.target && this.playerCallback != null) {
      this.playerCallback!!.onPlayerAcquired(player)
    }
  }

  // Playback's onInActive is equal to View's detach event or Activity stops.
  // Once it is inactive, its resource is considered freed and can be cleanup anytime.
  // Proper handling of in-active state must consider to: [1] Save any previous state (PlaybackInfo)
  // to cache, ready to reuse once coming back, [2] Consider to release allocated resource if there
  // is no other Manager manages the internal Playable.
  @CallSuper
  internal open fun onInActive() {
    for (callback in this.callbacks) {
      callback.onInActive(this)
    }
    val player = this.playerView
    if (player === this.target && this.playerCallback != null) {
      this.playerCallback!!.onPlayerReleased(player)
    }
  }

  // Being removed from Manager
  @CallSuper
  internal open fun onRemoved() {
    listenerHandler?.removeCallbacksAndMessages(null)
    for (callback in this.callbacks) {
      callback.onRemoved(this)
    }
    container.detachTarget(target)
    this.callbacks.clear()
    this.listeners.clear()
  }

  @CallSuper
  internal open fun onDestroyed() {
    this.listenerHandler = null
    this.removeCallback(this.playable)
    this.playerCallback = null
  }

  override fun toString(): String {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    return "$firstPart::$playable::$container"
  }

  interface Callback {
    fun onAdded(playback: Playback<*, *>) {}

    fun onActive(playback: Playback<*, *>) {}

    fun onInActive(playback: Playback<*, *>) {}

    fun onRemoved(playback: Playback<*, *>) {}
  }

  // To communicate with Playable only.
  internal interface PlayerCallback<PLAYER> {

    fun onPlayerAcquired(player: PLAYER)

    fun onPlayerReleased(player: PLAYER?)
  }
}
