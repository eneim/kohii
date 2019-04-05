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
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Playable.Companion.STATE_BUFFERING
import kohii.v1.Playable.Companion.STATE_END
import kohii.v1.Playable.Companion.STATE_IDLE
import kohii.v1.Playable.Companion.STATE_READY
import kohii.v1.Playable.RepeatMode
import kohii.v1.Playable.State
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Instance of this class will be tight to a Target. And that target is not reusable, so instance
 * of this class must not be passed around out of Activity's scope.
 *
 * @author eneim (2018/06/24).
 */
abstract class Playback<OUTPUT> internal constructor(
  internal val kohii: Kohii,
  internal val playable: Playable<OUTPUT>,
  val manager: PlaybackManager,
  val target: Any,
  internal val config: Playback.Config
) {

  companion object {
    const val TAG = "Kohii::PB"
    const val DELAY_INFINITE = -1L

    // Priority
    const val PRIORITY_HIGH = 1
    const val PRIORITY_NORMAL = 2
    const val PRIORITY_LOW = 3

    val VERTICAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, TargetHost.VERTICAL)
    }

    val HORIZONTAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, TargetHost.HORIZONTAL)
    }

    val BOTH_AXIS_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWidth(o2, TargetHost.BOTH_AXIS)
    }
  }

  @Retention(SOURCE)
  @IntDef(PRIORITY_HIGH, PRIORITY_NORMAL, PRIORITY_LOW)
  annotation class Priority

  open class Token {

    // = wantsToPlay()
    open fun shouldPlay() = false

    // Called by TargetHost, to know if a Playback should start preparing or not. True by default.
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
    val threshold: Float = 0.65F,
    val controller: Controller? = null,
    val playbackInfo: PlaybackInfo? = null
  )

  // Listeners for Playable. Playable will access these filed on demand.
  internal val volumeListeners by lazy { VolumeChangedListeners() }
  internal val playerListeners by lazy { PlayerEventListeners() }
  internal val errorListeners by lazy { ErrorListeners() }

  // This verification is later than expected, but we do not want to expose many internal things.
  // Ideally, targetHost should be verified before creating a Playback.
  // But doing so on a custom Playable/PlaybackCreator will requires PlaybackManager.findHostForTarget to be accessible
  // from client, which is not good.
  internal var targetHost = manager.findHostForTarget(target)
      ?: throw IllegalStateException("No host found for target: $target")

  private var listenerHandler: Handler? = null

  // Internal callbacks
  private val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  private val callbacks = CopyOnWriteArraySet<Callback>()

  // Token is comparable.
  internal abstract val token: Token

  internal abstract val outputHolder: OUTPUT?

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

  val tag = playable.tag

  // Notnull = User request for manual playback controller.
  // When comparing Playbacks, client must take this into account.
  var controller: Controller? = config.controller

  // Playback info update.

  // 1. Volume
  var volumeInfo: VolumeInfo
    get() {
      return playable.volumeInfo
    }
    set(value) {
      playable.setVolumeInfo(value)
    }

  fun seekTo(positionMs: Long) {
    this.playable.seekTo(positionMs)
  }

  @RepeatMode
  var repeatMode: Int = Playable.REPEAT_MODE_ONE
    set(value) {
      field = value
      this.playable.repeatMode = value
    }

  fun unbind() {
    this.unbindInternal()
    manager.performRemovePlayback(this)
  }

  // [END] Public API

  // Internal APIs

  // Lifecycle

  open fun compareWidth(
    other: Playback<*>,
    orientation: Int
  ): Int {
    return this.config.priority.compareTo(other.config.priority)
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
    listeners.forEach { it.onFirstFrameRendered(this@Playback) }
  }

  open fun prepare() {
    playable.prepare()
  }

  open fun play() {
    listeners.forEach { it.beforePlay(this@Playback) }
    playable.play()
  }

  open fun pause() {
    playable.pause()
    listeners.forEach { it.afterPause(this@Playback) }
  }

  open fun release() {
    playable.release()
  }

  @CallSuper
  internal open fun onCreated() {
    this.listenerHandler = Handler(Handler.Callback { message ->
      val playWhenReady = message.obj as Boolean
      when (message.what) {
        STATE_IDLE -> {
        }
        STATE_BUFFERING ->
          listeners.forEach { it.onBuffering(this@Playback, playWhenReady) }
        STATE_READY ->
          listeners.forEach {
            if (playWhenReady) it.onPlaying(
                this@Playback
            ) else it.onPaused(this@Playback)
          }
        STATE_END ->
          listeners.forEach { it.onCompleted(this@Playback) }
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
    targetHost.attachTarget(target)
  }

  // ~ View is attached
  @CallSuper
  internal open fun onActive() {
    for (callback in this.callbacks) {
      callback.onActive(this)
    }
    val player = this.outputHolder
    if (player != null && player === this.target) {
      this.playable.onPlayerActive(this, player)
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
    val player = this.outputHolder
    if (player === this.target) {
      this.playable.onPlayerInActive(this, player)
    }
  }

  // Being removed from Manager
  @CallSuper
  internal open fun onRemoved() {
    listenerHandler?.removeCallbacksAndMessages(null)
    for (callback in this.callbacks) {
      callback.onRemoved(this)
    }
    targetHost.detachTarget(target)
    this.callbacks.clear()
    this.listeners.clear()
  }

  @CallSuper
  internal open fun onDestroyed() {
    this.listenerHandler = null
    this.removeCallback(this.playable)
  }

  override fun toString(): String {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    return "$firstPart::$playable::$targetHost"
  }

  interface Callback {
    fun onAdded(playback: Playback<*>) {}

    fun onActive(playback: Playback<*>) {}

    fun onInActive(playback: Playback<*>) {}

    fun onRemoved(playback: Playback<*>) {}
  }

  // Test scenarios:
  // 1. User set Controller, User scroll Playback off screen, User remove Controller.
  interface Controller {
    // false = full manual.
    // true = half manual.
    // When true:
    // - If user starts a play, it will not be paused unless Playback is not visible enough (controlled by Config)
    // - If user pauses a playing Playback, it will not be played unless user resume it. Work with config change.
    // - If user scrolls so that a Playback is not visible enough, system will pause the Playback.
    // - If user scrolls a paused Playback so that it is visible enough, system will: play it if it was previously playing,
    // or pause it if it was paused before (= do nothing).
    fun allowsSystemControl(): Boolean = true
  }
}
