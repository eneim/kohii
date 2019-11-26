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

package kohii.v1.core

import android.graphics.Rect
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.STARTED
import kohii.v1.BuildConfig
import kohii.v1.core.Bucket.Companion.BOTH_AXIS
import kohii.v1.core.Bucket.Companion.HORIZONTAL
import kohii.v1.core.Bucket.Companion.NONE_AXIS
import kohii.v1.core.Bucket.Companion.VERTICAL
import kohii.v1.core.Common.STATE_BUFFERING
import kohii.v1.core.Common.STATE_ENDED
import kohii.v1.core.Common.STATE_IDLE
import kohii.v1.core.Common.STATE_READY
import kohii.v1.logDebug
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo
import java.util.ArrayDeque
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max
import kotlin.properties.Delegates

abstract class Playback(
  internal val manager: Manager,
  internal val bucket: Bucket,
  val container: ViewGroup,
  val config: Config = Config()
) : PlayerEventListener,
    ErrorListener {

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
    val containerRect: Rect // Relative Rect to its Bucket's root View.
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
    val repeatMode: Int = Common.REPEAT_MODE_OFF,
    val controller: Controller? = null,
    val artworkHintListener: ArtworkHintListener? = null,
    val callbacks: Set<Callback> = emptySet()
  )

  override fun toString(): String {
    return "${super.toString()}, [$playable], [${token.areaOffset}, ${token.containerRect}]"
  }

  protected open fun updateToken(): Token {
    "Playback#updateToken $this".logDebug()
    val containerRect = Rect()
    if (!lifecycleState.isAtLeast(STARTED)) return Token(
        config.threshold, -1F, containerRect
    )
    if (!ViewCompat.isAttachedToWindow(container)) {
      return Token(config.threshold, -1F, containerRect)
    }

    val visible = container.getGlobalVisibleRect(containerRect)
    if (!visible) return Token(
        config.threshold, -1F, containerRect
    )

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
  private val listeners = ArrayDeque<StateListener>()
  private var artworkHintListener: ArtworkHintListener? = null

  internal open fun acquireRenderer(): Any? {
    val playable = this.playable
    requireNotNull(playable)
    val provider: RendererProvider = manager.group.findRendererProvider(playable)
    return provider.acquireRenderer(this, playable.media)
  }

  internal open fun releaseRenderer(
    renderer: Any?
  ) {
    val playable = this.playable
    requireNotNull(playable)
    val provider: RendererProvider = manager.group.findRendererProvider(playable)
    return provider.releaseRenderer(this, playable.media, renderer)
  }

  internal fun attachRenderer(renderer: Any?): Boolean {
    "Playback#attachRenderer $renderer $this".logDebug()
    return onAttachRenderer(renderer)
  }

  internal fun detachRenderer(renderer: Any?): Boolean {
    "Playback#detachRenderer $renderer $this".logDebug()
    return onDetachRenderer(renderer)
  }

  // Return `true` to indicate that the Renderer is safely attached to container and
  // can be used by the Playable.
  protected abstract fun onAttachRenderer(renderer: Any?): Boolean

  // Return `true` to indicate that the Renderer is safely detached from container and
  // Playable should not use it any further. RendererProvider will then release the Renderer with
  // proper mechanism (eg: put it back to Pool for reuse).
  protected abstract fun onDetachRenderer(renderer: Any?): Boolean

  internal fun onAdded() {
    "Playback#onAdded $this".logDebug()
    playbackState = STATE_ADDED
    callbacks.forEach { it.onAdded(this) }
    artworkHintListener = config.artworkHintListener
    bucket.addContainer(this.container)
  }

  internal fun onRemoved() {
    "Playback#onRemoved $this".logDebug()
    playbackState = STATE_REMOVED
    bucket.removeContainer(this.container)
    artworkHintListener = null
    callbacks.onEach { it.onRemoved(this) }
        .clear()
    listeners.clear()
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

  @CallSuper
  internal open fun onActive() {
    "Playback#onActive $this".logDebug()
    playbackState = STATE_ACTIVE
    callbacks.forEach { it.onActive(this) }
    artworkHintListener?.onArtworkHint(
        playable?.isPlaying() == false,
        playbackInfo.resumePosition,
        playerState
    )
  }

  @CallSuper
  internal open fun onInActive() {
    "Playback#onInActive $this".logDebug()
    playbackState = STATE_INACTIVE
    artworkHintListener?.onArtworkHint(true, playbackInfo.resumePosition, playerState)
    playable?.considerReleaseRenderer(this)
    callbacks.forEach { it.onInActive(this) }
  }

  @CallSuper
  internal open fun onPlay() {
    "Playback#onPlay $this".logDebug()
    container.keepScreenOn = true
    // listeners.forEach { it.beforePlay(this) }
    artworkHintListener?.onArtworkHint(
        playerState == STATE_ENDED, playbackInfo.resumePosition, playerState
    )
  }

  @CallSuper
  internal open fun onPause() {
    container.keepScreenOn = false
    "Playback#onPause $this".logDebug()
    artworkHintListener?.onArtworkHint(true, playbackInfo.resumePosition, playerState)
    // listeners.forEach { it.afterPause(this) }
  }

  // Will be updated everytime 'onRefresh' is called.
  private var _token: Token =
    Token(config.threshold, -1F, Rect())

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

  // The smaller, the closer it is to be selected to Play.
  // Consider to prepare the underline Playable for low enough distance, and release it otherwise.
  // This value is updated by Group. In active Playback always has Int.MAX_VALUE distance.
  internal var distanceToPlay: Int by Delegates.observable(
      Int.MAX_VALUE,
      onChange = { _, from, to ->
        if (from == to) return@observable
        "Playback#distanceToPlay $from --> $to, $this".logDebug()
        playable?.onDistanceChanged(this, from, to)
      })

  internal var volumeInfoUpdater: VolumeInfo by Delegates.observable(
      initialValue = bucket.volumeInfo,
      onChange = { _, from, to ->
        if (from == to) return@observable
        "Playback#volumeInfo $from --> $to, $this".logDebug()
        playable?.onVolumeInfoChanged(this, from, to)
      }
  )

  init {
    volumeInfoUpdater = bucket.volumeInfo
  }

  internal var playable: Playable? = null

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

  // Public APIs

  val tag = config.tag

  private val playerState: Int
    get() = playable?.playerState ?: STATE_IDLE

  val volumeInfo: VolumeInfo
    get() = volumeInfoUpdater

  private val playbackInfo: PlaybackInfo
    get() = playable?.playbackInfo ?: PlaybackInfo()

  val containerRect: Rect
    get() = token.containerRect

  fun addCallback(callback: Callback) {
    "Playback#addCallback $callback, $this".logDebug()
    this.callbacks.push(callback)
  }

  fun removeCallback(callback: Callback?) {
    "Playback#removeCallback $callback, $this".logDebug()
    this.callbacks.remove(callback)
  }

  fun addStateListener(listener: StateListener) {
    "Playback#addStateListener $listener, $this".logDebug()
    this.listeners.add(listener)
  }

  fun removeStateListener(listener: StateListener?) {
    "Playback#removeStateListener $listener, $this".logDebug()
    this.listeners.remove(listener)
  }

  fun unbind() {
    "Playback#unbind, $this".logDebug()
    container.post {
      playable?.onUnbind(this) ?: manager.removePlayback(this)
    }
  }

  fun rewind(refresh: Boolean = true) {
    playable?.onReset()
    if (refresh) manager.refresh()
  }

  // PlayerEventListener

  override fun onPlayerStateChanged(
    playWhenReady: Boolean,
    playbackState: Int
  ) {
    "Playback#onPlayerStateChanged $playWhenReady - $playbackState, $this".logDebug()
    when (playbackState) {
      STATE_IDLE -> {
      }
      STATE_BUFFERING -> {
        listeners.forEach { it.onBuffering(this@Playback, playWhenReady) }
      }
      STATE_READY -> {
        listeners.forEach {
          if (playWhenReady) it.onPlaying(this@Playback) else it.onPaused(this@Playback)
        }
      }
      STATE_ENDED -> {
        listeners.forEach { it.onEnded(this@Playback) }
      }
    }
    artworkHintListener?.onArtworkHint(
        playerState == STATE_ENDED || playerState == STATE_IDLE,
        playbackInfo.resumePosition, playerState
    )
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
    listeners.forEach { it.onRendered(this) }
  }

  // ErrorListener

  override fun onError(error: Exception) {
    "Playback#onError $error, $this".logDebug()
    listeners.forEach { it.onError(this, error) }
    if (BuildConfig.DEBUG) error.printStackTrace()
  }

  interface StateListener {

    /** Called when a Video is rendered on the Surface for the first time */
    @JvmDefault
    fun onRendered(playback: Playback) {
    }

    /**
     * Called when buffering status of the playback is changed.
     *
     * @param playWhenReady true if the Video will start playing once buffered enough, false otherwise.
     */
    @JvmDefault
    fun onBuffering(
      playback: Playback,
      playWhenReady: Boolean
    ) {
    } // ExoPlayer state: 2

    /** Called when the Video starts playing */
    @JvmDefault
    fun onPlaying(playback: Playback) {
    } // ExoPlayer state: 3, play flag: true

    /** Called when the Video is paused */
    @JvmDefault
    fun onPaused(playback: Playback) {
    } // ExoPlayer state: 3, play flag: false

    /** Called when the Video finishes its playback */
    @JvmDefault
    fun onEnded(playback: Playback) {
    } // ExoPlayer state: 4

    @JvmDefault
    fun onVideoSizeChanged(
      playback: Playback,
      width: Int,
      height: Int,
      unAppliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float
    ) {
    }

    @JvmDefault
    fun onError(
      playback: Playback,
      exception: Exception
    ) {
    }
  }

  interface Callback {

    @JvmDefault
    fun onActive(playback: Playback) {
    }

    @JvmDefault
    fun onInActive(playback: Playback) {
    }

    @JvmDefault
    fun onAdded(playback: Playback) {
    }

    @JvmDefault
    fun onRemoved(playback: Playback) {
    }

    @JvmDefault
    fun onAttached(playback: Playback) {
    }

    @JvmDefault
    fun onDetached(playback: Playback) {
    }
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
    @JvmDefault
    fun kohiiCanPause(): Boolean = true

    // - Allow System to start a Playback.
    // When true:
    // - Kohii can start a Playback automatically. But once user pause it manually, Only user can resume it,
    // Kohii should never start/resume the Playback.
    @JvmDefault
    fun kohiiCanStart(): Boolean = false
  }

  interface ArtworkHintListener {

    fun onArtworkHint(
      shouldShow: Boolean,
      position: Long,
      state: Int
    )
  }
}
