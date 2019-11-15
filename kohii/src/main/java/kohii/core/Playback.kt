/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
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

package kohii.core

import android.graphics.Rect
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.android.exoplayer2.Player
import kohii.core.Host.Companion.BOTH_AXIS
import kohii.core.Host.Companion.HORIZONTAL
import kohii.core.Host.Companion.NONE_AXIS
import kohii.core.Host.Companion.VERTICAL
import kohii.logDebug
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.BuildConfig
import kohii.v1.ErrorListener
import kohii.v1.PlayerEventListener
import java.util.ArrayDeque
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max
import kotlin.properties.Delegates

abstract class Playback(
  internal val manager: Manager,
  internal val host: Host,
  internal val config: Config = Config(),
  val container: ViewGroup
) : PlayerEventListener, ErrorListener, Switch.Callback {

  companion object {
    @Suppress("unused")
    const val DELAY_INFINITE = -1L

    internal val CENTER_X: Comparator<Token> by lazy(NONE) {
      Comparator<Token> { o1, o2 ->
        compareValues(o1.containerRect.centerX(), o2.containerRect.centerX())
      }
    }

    internal val CENTER_Y: Comparator<Token> by lazy(NONE) {
      Comparator<Token> { o1, o2 ->
        compareValues(o1.containerRect.centerY(), o2.containerRect.centerY())
      }
    }

    internal val VERTICAL_COMPARATOR by lazy(NONE) {
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, VERTICAL) }
    }

    internal val HORIZONTAL_COMPARATOR by lazy(NONE) {
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, HORIZONTAL) }
    }

    internal val BOTH_AXIS_COMPARATOR by lazy(NONE) {
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, BOTH_AXIS) }
    }

    private const val STATE_CREATED = -1
    private const val STATE_REMOVED = 0
    private const val STATE_ADDED = 1
    private const val STATE_DETACHED = 2
    private const val STATE_ATTACHED = 3
    private const val STATE_INACTIVE = 4
    private const val STATE_ACTIVE = 5
  }

  class Token(
    private val threshold: Float = 0.65F,
    @FloatRange(from = -1.0, to = 1.0)
    val areaOffset: Float, // -1 ~ < 0 : inactive or detached, 0 ~ 1: active
    val containerRect: Rect // Relative Rect to its Host's root View.
  ) {

    fun shouldPrepare(): Boolean {
      return areaOffset >= 0
    }

    fun shouldPlay(): Boolean {
      return areaOffset >= threshold
    }
  }

  data class Config(
    val tag: Any = Master.NO_TAG,
    val delay: Int = 0,
    val threshold: Float = 0.65F,
    val preload: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val controller: Controller? = null,
    val callbacks: Array<Callback> = emptyArray()
  ) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Config

      if (delay != other.delay) return false
      if (controller != other.controller) return false
      if (!callbacks.contentEquals(other.callbacks)) return false
      return true
    }

    override fun hashCode(): Int {
      var result = delay
      result = 31 * result + (controller?.hashCode() ?: 0)
      result = 31 * result + callbacks.contentHashCode()
      return result
    }
  }

  override fun toString(): String {
    return "${super.toString()}, [$rendererHolderListener], [${token.areaOffset}, ${token.containerRect}]"
  }

  protected open fun updateToken(): Token {
    "Playback#updateToken $this".logDebug()
    val containerRect = Rect()
    if (!lifecycleState.isAtLeast(STARTED)) return Token(config.threshold, -1F, containerRect)
    if (!ViewCompat.isAttachedToWindow(container)) {
      return Token(config.threshold, -1F, containerRect)
    }

    val visible = container.getGlobalVisibleRect(containerRect)
    if (!visible) return Token(config.threshold, -1F, containerRect)

    val drawArea = with(Rect()) {
      container.getDrawingRect(this)
      container.clipBounds?.let {
        this.intersect(it)
      }
      width() * height()
    }

    val offset: Float =
      if (drawArea > 0)
        (containerRect.width() * containerRect.height()) / drawArea.toFloat()
      else
        0F
    return Token(config.threshold, offset, containerRect)
  }

  private val callbacks = ArrayDeque<Callback>()
  private val listeners = ArrayDeque<PlaybackListener>()

  // Return **true** to indicate that the Renderer is safely attached and
  // can be used by the Playable.
  abstract fun <RENDERER : Any> onAttachRenderer(renderer: RENDERER?): Boolean

  // Return **true** to indicate that the Renderer is safely detached and
  // Playable should not use it any further. RendererProvider will then release the Renderer with
  // proper mechanism (eg: put it back to Pool for reuse).
  abstract fun <RENDERER : Any> onDetachRenderer(renderer: RENDERER?): Boolean

  internal fun onAdded() {
    "Playback#onAdded $this".logDebug()
    playbackState = STATE_ADDED
    callbacks.forEach { it.onAdded(this) }
    host.addContainer(this.container)
  }

  internal fun onRemoved() {
    "Playback#onRemoved $this".logDebug()
    playbackState = STATE_REMOVED
    host.removeContainer(this.container)
    callbacks.onEach { it.onRemoved(this) }
        .clear()
  }

  internal fun onAttached() {
    "Playback#onAttached $this".logDebug()
    playbackState = STATE_ATTACHED
    callbacks.forEach { it.onAttached(this) }
  }

  internal fun onDetached() {
    "Playback#onDetached $this".logDebug()
    playbackState = STATE_DETACHED
    callbacks.forEach { it.onDetached(this) }
  }

  internal fun attachRenderer(renderer: Any?): Boolean {
    "Playback#attachRenderer $renderer $this".logDebug()
    return onAttachRenderer(renderer)
  }

  internal fun detachRenderer(renderer: Any?): Boolean {
    "Playback#detachRenderer $renderer $this".logDebug()
    // TODO 'renderer' seems to be always null...
    return onDetachRenderer(renderer)
  }

  @CallSuper
  internal open fun onActive() {
    "Playback#onActive $this".logDebug()
    playbackState = STATE_ACTIVE
    callbacks.forEach { it.onActive(this) }
  }

  @CallSuper
  internal open fun onInActive() {
    "Playback#onInActive $this".logDebug()
    playbackState = STATE_INACTIVE
    rendererHolderListener?.considerReleaseRenderer(this)
    callbacks.forEach { it.onInActive(this) }
  }

  @CallSuper
  internal open fun onPlay() {
    "Playback#onPlay $this".logDebug()
    listeners.forEach { it.beforePlay(this) }
  }

  @CallSuper
  internal open fun onPause() {
    "Playback#onPause $this".logDebug()
    listeners.forEach { it.afterPause(this) }
  }

  // Will be updated everytime 'sessionFlag' changes
  private var _token: Token = Token(config.threshold, -1F, Rect())

  internal val token: Token
    get() = _token

  internal fun onRefresh() {
    "Playback#onRefresh $this".logDebug()
    _token = updateToken()
  }

  private var playbackState: Int = STATE_CREATED

  internal val isAttached: Boolean
    get() = playbackState >= STATE_ATTACHED

  internal val isActive: Boolean
    get() = playbackState >= STATE_ACTIVE

  internal var lifecycleState: State = State.INITIALIZED

  internal var distanceListener: DistanceListener? = null
  // The smaller, the closer it is to be selected to Play.
  // Consider to prepare the underline Playable for low enough distance, and release it otherwise.
  // This value is updated by Group. In active Playback always has Int.MAX_VALUE distance.
  internal var distanceToPlay: Int by Delegates.observable(
      Int.MAX_VALUE,
      onChange = { _, from, to ->
        if (from == to) return@observable
        "Playback#distanceToPlay $from --> $to, $this".logDebug()
        distanceListener?.onDistanceChanged(this, from, to)
      })

  internal var volumeInfoListener: VolumeInfoListener? = null
  internal var volumeInfoUpdater: VolumeInfo by Delegates.observable(
      initialValue = host.volumeInfo,
      onChange = { _, from, to ->
        if (from == to) return@observable
        "Playback#volumeInfo $from --> $to, $this".logDebug()
        volumeInfoListener?.onVolumeInfoChange(this, from, to)
      }
  )

  init {
    volumeInfoUpdater = host.volumeInfo
  }

  internal var playbackInfoListener: PlaybackInfoListener? = null
  internal var rendererHolderListener: RendererHolderListener? = null
  internal var unbinder: Unbinder? = null

  internal fun compareWith(
    other: Playback,
    orientation: Int
  ): Int {
    "Playback#compareWith $this $other, $this".logDebug()
    val thisToken = this.token
    val thatToken = other.token

    val compareVertically by lazy(NONE) { CENTER_Y.compare(thisToken, thatToken) }
    val compareHorizontally by lazy(NONE) { CENTER_X.compare(thisToken, thatToken) }

    var result = when (orientation) {
      VERTICAL -> compareVertically
      HORIZONTAL -> compareHorizontally
      BOTH_AXIS -> max(compareVertically, compareHorizontally)
      NONE_AXIS -> max(compareVertically, compareHorizontally)
      else -> 0
    }

    if (result == 0) result = compareValues(thisToken.areaOffset, thatToken.areaOffset)
    return result
  }

  override fun onSwitch(
    switch: Switch,
    from: Boolean,
    to: Boolean
  ) {
    "Playback#onSwitch $switch $from --> $to, $this".logDebug()
    TODO(
        "onSwitch not implemented"
    ) // To change body of created functions use File | Settings | File Templates.
  }

  // Public APIs

  val tag = config.tag

  val volumeInfo: VolumeInfo
    get() = volumeInfoUpdater

  val playbackInfo: PlaybackInfo
    get() = playbackInfoListener?.playbackInfo ?: PlaybackInfo()

  fun addCallback(callback: Callback) {
    "Playback#addCallback $callback, $this".logDebug()
    this.callbacks.push(callback)
  }

  fun removeCallback(callback: Callback?) {
    "Playback#removeCallback $callback, $this".logDebug()
    this.callbacks.remove(callback)
  }

  fun addPlaybackListener(listener: PlaybackListener) {
    "Playback#addPlaybackListener $listener, $this".logDebug()
    this.listeners.add(listener)
  }

  fun removePlaybackListener(listener: PlaybackListener?) {
    "Playback#removePlaybackListener $listener, $this".logDebug()
    this.listeners.remove(listener)
  }

  fun unbind() {
    "Playback#unbind, $this".logDebug()
    unbinder?.onUnbind(this) ?: manager.removePlayback(this)
  }

  // PlayerEventListener

  override fun onPlayerStateChanged(
    playWhenReady: Boolean,
    playbackState: Int
  ) {
    "Playback#onPlayerStateChanged $playWhenReady - $playbackState, $this".logDebug()
    container.keepScreenOn = playWhenReady
    when (playbackState) {
      Player.STATE_IDLE -> {
      }
      Player.STATE_BUFFERING -> {
        listeners.forEach { it.onBuffering(this@Playback, playWhenReady) }
      }
      Player.STATE_READY -> {
        listeners.forEach {
          if (playWhenReady) it.onPlay(this@Playback) else it.onPause(this@Playback)
        }
      }
      Player.STATE_ENDED -> {
        listeners.forEach { it.onEnd(this@Playback) }
      }
    }
  }

  override fun onVideoSizeChanged(
    width: Int,
    height: Int,
    unappliedRotationDegrees: Int,
    pixelWidthHeightRatio: Float
  ) {
    "Playback#onVideoSizeChanged $width × $height, $this".logDebug()
    listeners.forEach {
      it.onVideoSizeChanged(this, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
    }
  }

  override fun onRenderedFirstFrame() {
    "Playback#onRenderedFirstFrame, $this".logDebug()
    listeners.forEach { it.onFirstFrameRendered(this) }
  }

  // ErrorListener

  override fun onError(error: Exception) {
    "Playback#onError $error, $this".logDebug()
    listeners.forEach { it.onError(this, error) }
    if (BuildConfig.DEBUG) error.printStackTrace()
  }

  interface PlaybackListener {

    /** Called when a Video is rendered on the Surface for the first time */
    fun onFirstFrameRendered(playback: Playback) {}

    /**
     * Called when buffering status of the playback is changed.
     *
     * @param playWhenReady true if the Video will start playing once buffered enough, false otherwise.
     */
    fun onBuffering(
      playback: Playback,
      playWhenReady: Boolean
    ) {
    } // ExoPlayer state: 2

    /** Called when the Video starts playing */
    fun onPlay(playback: Playback) {} // ExoPlayer state: 3, play flag: true

    /** Called when the Video is paused */
    fun onPause(playback: Playback) {} // ExoPlayer state: 3, play flag: false

    /** Called when the Video finishes its playback */
    fun onEnd(playback: Playback) {} // ExoPlayer state: 4

    fun beforePlay(playback: Playback) {}

    fun afterPause(playback: Playback) {}

    fun onVideoSizeChanged(
      playback: Playback,
      width: Int,
      height: Int,
      unAppliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float
    ) {
    }

    fun onError(
      playback: Playback,
      exception: Exception
    ) {
    }
  }

  interface Callback {

    fun onActive(playback: Playback) {}

    fun onInActive(playback: Playback) {}

    fun onAdded(playback: Playback) {}

    fun onRemoved(playback: Playback) {}

    fun onAttached(playback: Playback) {}

    fun onDetached(playback: Playback) {}
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

  // Below interfaces are to communicate with Playable.

  internal interface DistanceListener {

    fun onDistanceChanged(
      playback: Playback,
      from: Int,
      to: Int
    )
  }

  internal interface VolumeInfoListener {

    fun onVolumeInfoChange(
      playback: Playback,
      from: VolumeInfo,
      to: VolumeInfo
    )
  }

  internal interface PlaybackInfoListener {

    var playbackInfo: PlaybackInfo
  }

  /**
   * Once the Playback finds it is good time for the Listener to request/release the Renderer, it
   * will trigger these calls to send that signal. The 'good time' can varies due to the actual
   * use case. In Kohii, there are 2 following cases:
   * - The Playback's Container is also the renderer. In this case, the Container/Renderer will
   * always be there. We suggest that the Listener should request for the Renderer as soon as
   * possible, and release it as late as possible. The proper place to do that are when the
   * Playback becomes active (onActive()) and inactive (onInActive()).
   *
   * @see [StaticViewRendererPlayback]
   *
   * - The Playback's Container is not the Renderer. In this case, the Renderer is expected
   * to be created on demand and release as early as possible, so that Kohii can reuse it for
   * other Playback as soon as possible. We suggest that the Listener should request for
   * the Renderer just right before the Playback starts (onPlay()), and release the Renderer
   * just right after the Playback pauses (onPause()).
   *
   * @see [DynamicViewRendererPlayback]
   * @see [DynamicFragmentRendererPlayback]
   */
  internal interface RendererHolderListener {

    fun considerRequestRenderer(playback: Playback)

    fun considerReleaseRenderer(playback: Playback)
  }

  internal interface Unbinder {

    fun onUnbind(playback: Playback)
  }
}
