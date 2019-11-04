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

import androidx.annotation.CallSuper
import com.google.android.exoplayer2.PlaybackParameters
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Playable.Companion.STATE_BUFFERING
import kohii.v1.Playable.Companion.STATE_END
import kohii.v1.Playable.Companion.STATE_IDLE
import kohii.v1.Playable.Companion.STATE_READY
import kohii.v1.Playable.RepeatMode
import kohii.v1.Playable.State
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Instance of this class will be tight to a Target. And that container is not reusable, so instance
 * of this class must not be passed around out of Activity's scope.
 *
 * @author eneim (2018/06/24).
 */
abstract class Playback<RENDERER : Any> internal constructor(
  internal val kohii: Kohii,
  internal val playable: Playable<RENDERER>,
  val manager: PlaybackManager,
  val container: Any, // TODO rename to 'container'
  val config: Config
) : TargetHolder, PlayableHolder, PlayerEventListener {

  companion object {
    const val TAG = BuildConfig.LIBRARY_PACKAGE_NAME
    const val DELAY_INFINITE = -1L

    val VERTICAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, TargetHost.VERTICAL)
    }

    val HORIZONTAL_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, TargetHost.HORIZONTAL)
    }

    val BOTH_AXIS_COMPARATOR = Comparator<Playback<*>> { o1, o2 ->
      return@Comparator o1.compareWith(o2, TargetHost.BOTH_AXIS)
    }
  }

  open class Token {

    // = wantsToPlay()
    open fun shouldPlay() = false

    // Called by TargetHost, to know if a Playback should start preparing or not. True by default.
    // = allowsToPlay(player)
    open fun shouldPrepare() = false
  }

  data class Config(
    val delay: Int = 0,
      // Indicator to used to judge of a Playback should be played or not.
      // This doesn't warranty that it will be played, it just to make the Playback be a candidate
      // to start a playback.
      // In ViewPlayback, this is equal to visible area offset of the video container View.
    val threshold: Float = 0.65F,
    val disabled: () -> Boolean = { false }, // If false, do not care about this Playback. Stateful, can leak
    val controller: Controller? = null, // stateful, can leak
    val playbackInfo: PlaybackInfo? = null,
    @RepeatMode val repeatMode: Int = Playable.REPEAT_MODE_OFF,
    val parameters: PlaybackParameters = PlaybackParameters.DEFAULT,
    val keepScreenOn: Boolean = true,
    val callback: Callback? = null // stateful, can leak
  )

  // Listeners for Playable. Playable will access these filed on demand.
  internal val volumeListeners by lazy { VolumeChangedListeners() }
  internal val playerListeners by lazy { PlayerEventListeners() }
  internal val errorListeners by lazy { ErrorListeners() }

  // This verification is later than expected, but we do not want to expose many internal things.
  // Ideally, targetHost should be verified before creating a Playback.
  // But doing so on a custom Playable/PlaybackCreator will requires PlaybackManager.findHostForContainer
  // to be accessible from client, which is not good.
  internal var targetHost = manager.findHostForContainer(container)
      ?: throw IllegalStateException("No host found for container: $container")

  // Internal callbacks
  private val listeners = CopyOnWriteArraySet<PlaybackEventListener>()
  private val callbacks = CopyOnWriteArraySet<Callback>()

  // Token is comparable.
  internal abstract val token: Token

  internal abstract val renderer: RENDERER?

  // [BEGIN] Public API

  fun addPlaybackEventListener(listener: PlaybackEventListener) {
    if (this.listeners.add(listener)) {
      when (this.playbackState) {
        STATE_IDLE -> {
        }
        STATE_BUFFERING ->
          listener.onBuffering(this@Playback, playable.isPlaying())
        STATE_READY ->
          if (playable.isPlaying()) listener.onPlay(this@Playback)
          else listener.onPause(this@Playback)
        STATE_END -> listener.onEnd(this@Playback)
      }
    }
  }

  fun removePlaybackEventListener(listener: PlaybackEventListener?) {
    this.listeners.remove(listener)
  }

  internal fun addCallback(callback: Callback) {
    this.callbacks.add(callback)
  }

  internal fun removeCallback(callback: Callback?) {
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
  val controller: Controller? = config.controller

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

  val playbackState: Int
    get() = playable.playbackState

  fun play() {
    playable.play()
  }

  fun pause() {
    playable.pause()
  }

  fun rewind() {
    playable.reset()
  }

  fun unbind() {
    manager.unbind(this)
    this.onUnbind()
  }

  // [END] Public API

  // Internal APIs

  // Lifecycle

  protected open fun compareWith(
    other: Playback<*>,
    orientation: Int
  ): Int {
    return 0
  }

  @CallSuper
  open fun onUnbind() {
    // no-ops. for child class to override.
  }

  // Used by subclasses to dispatch internal event listeners
  @CallSuper
  override fun onPlayerStateChanged(playWhenReady: Boolean, @State playbackState: Int) {
    when (playbackState) {
      STATE_IDLE -> {
      }
      STATE_BUFFERING ->
        listeners.forEach { it.onBuffering(this@Playback, playWhenReady) }
      STATE_READY ->
        listeners.forEach {
          if (playWhenReady) it.onPlay(
              this@Playback
          ) else it.onPause(this@Playback)
        }
      STATE_END ->
        listeners.forEach { it.onEnd(this@Playback) }
    }
  }

  @CallSuper
  override fun onRenderedFirstFrame() {
    listeners.forEach { it.onFirstFrameRendered(this@Playback) }
  }

  // Scenario: in first bind of RV, VH's itemView has null Parent, but might has LayoutParams
  // of RV, we 'temporarily' ask any RV to accept it. When it is updated, we find the correct one.
  // This operation should not happen always, ideally up to 1 time.
  internal fun doubleCheckHost() {
    val properHost = manager.targetHosts.firstOrNull { it.accepts(this.container) }
    if (properHost != null && this.targetHost !== properHost) {
      this.targetHost.detachContainer(this.container)
      this.targetHost = properHost
      this.targetHost.attachContainer(this.container)
    }
  }

  internal fun playInternal() {
    this.beforePlayInternal()
    this.play()
  }

  internal fun pauseInternal() {
    this.pause()
    this.afterPauseInternal()
  }

  internal fun release() {
    playable.onRelease(this)
  }

  @CallSuper
  protected open fun beforePlayInternal() {
    listeners.forEach { it.beforePlay(this@Playback) }
  }

  @CallSuper
  protected open fun afterPauseInternal() {
    listeners.forEach { it.afterPause(this@Playback) }
  }

  // Being added to Manager
  // The container may not be attached to View/Window yet.
  @CallSuper
  internal open fun onAdded() {
    this.playable.onAdded(this)
    for (callback in this.callbacks) {
      callback.onAdded(this)
    }
    targetHost.attachContainer(container)
  }

  // Being removed from Manager
  @CallSuper
  internal open fun onRemoved() {
    if (playable.playback === this) playable.playback = null
    this.playable.onRemoved(this)
    for (callback in this.callbacks) {
      callback.onRemoved(this)
    }
    targetHost.detachContainer(container)
    this.callbacks.clear()
    this.listeners.clear()
  }

  private val lazyString by lazy(NONE) {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    val tagString = tag.toString()
    val tagLog = tagString.substring(tagString.length - 4)
    "$firstPart::$tagLog"
  }

  override fun toString(): String {
    return lazyString
  }

  interface Callback {

    /** Called when the Playback is added to the PlaybackManager */
    fun onAdded(playback: Playback<*>) {}

    /** Called when the Playback becomes active. It is equal to that the container PlayerView is attached to the Window */
    fun onActive(playback: Playback<*>) {}

    /** Called when the Playback becomes inactive. It is equal to that the container PlayerView is detached from the Window */
    fun onInActive(playback: Playback<*>) {}

    /** Called when the Playback is removed from the PlaybackManager */
    fun onRemoved(playback: Playback<*>) {}
  }

  interface Controller {
    // false = full manual.
    // true = half manual.
    // When true:
    // - If user starts a Playback, it will not be paused until Playback is not visible enough
    // (controlled by Playback.Config), or user starts other Playback (priority overridden).
    // - If user pauses a Playback, it will not be played until user resumes it.
    // - If user scrolls a Playback so that a it is not visible enough, system will pause the Playback.
    // - If user scrolls a paused Playback so that it is visible enough, system will: play it if it was previously played by User,
    // or pause it if it was paused by User before (= do nothing).
    fun kohiiCanPause(): Boolean = true

    // - Allow System to start a Playback.
    // When true:
    // - Kohii can start a Playback automatically. But once user pause it manually, Only user can resume it,
    // Kohii should never start/resume the Playback.
    fun kohiiCanStart(): Boolean = false
  }

  fun onTargetAttached() {}

  fun onTargetDetached() {}

  @CallSuper
  open fun onActive() {
    for (callback in this.callbacks) {
      callback.onActive(this)
    }

    if (playable.manager === this.manager) {
      // val current = manager.mapTargetToPlayback[this.container]
      // if (current?.playable !== this.playable) return
      val player = this.renderer
      if (player === this.container) {
        this.playable.onPlaybackActive(this, player)
      }
    }
  }

  @CallSuper
  open fun onInActive() {
    for (callback in this.callbacks) {
      callback.onInActive(this)
    }

    if (playable.manager === this.manager) {
      // val current = manager.mapTargetToPlayback[this.container]
      // if (current?.playable !== this.playable) return
      val player = this.renderer
      if (player === this.container) {
        this.playable.onPlaybackInActive(this, player)
      }
    }
  }
}
