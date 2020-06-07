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
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.android.exoplayer2.Player
import kohii.v1.BuildConfig
import kohii.v1.core.Bucket.Companion.BOTH_AXIS
import kohii.v1.core.Bucket.Companion.HORIZONTAL
import kohii.v1.core.Bucket.Companion.NONE_AXIS
import kohii.v1.core.Bucket.Companion.VERTICAL
import kohii.v1.core.Common.STATE_ENDED
import kohii.v1.core.Common.STATE_IDLE
import kohii.v1.internal.PlayerParametersChangeListener
import kohii.v1.logDebug
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo
import java.util.ArrayDeque
import kotlin.math.max

/**
 * Common interface of an object that defines the connection between a [Playable] and a [ViewGroup]
 * as an [container]. When the [Master] receive a request to bind a Video to a [ViewGroup], it first
 * produce a [Playable] (either by acquiring from cache, or creating a new one) for the Video, and
 * then creating a new instance of this object if needed.
 *
 * A [Playback] does not only store the information about the [ViewGroup] as [container], and the
 * [Playable], but also help the [Master] to know if the [Playable] should be played or paused, by
 * telling it to how much the [container] is visible and other conditions.
 *
 * @see [kohii.v1.internal.StaticViewRendererPlayback]
 * @see [kohii.v1.internal.DynamicViewRendererPlayback]
 * @see [kohii.v1.internal.DynamicFragmentRendererPlayback]
 */
abstract class Playback(
  val manager: Manager,
  val bucket: Bucket,
  val container: ViewGroup,
  val config: Config = Config()
) : PlayableContainer, PlayerEventListener, ErrorListener {

  companion object {
    @Suppress("unused")
    const val DELAY_INFINITE = -1L

    internal val CENTER_X: Comparator<Token> = Comparator { o1, o2 ->
      compareValues(o1.containerRect.centerX(), o2.containerRect.centerX())
    }

    internal val CENTER_Y: Comparator<Token> = Comparator { o1, o2 ->
      compareValues(o1.containerRect.centerY(), o2.containerRect.centerY())
    }

    internal val VERTICAL_COMPARATOR =
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, VERTICAL) }

    internal val HORIZONTAL_COMPARATOR =
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, HORIZONTAL) }

    internal val BOTH_AXIS_COMPARATOR =
      Comparator<Playback> { o1, o2 -> o1.compareWith(o2, BOTH_AXIS) }

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
    val containerRect: Rect, // Relative Rect to its Bucket's root View.
    val containerWidth: Int,
    val containerHeight: Int
  ) {

    internal fun shouldPrepare(): Boolean {
      return areaOffset >= 0
    }

    internal fun shouldPlay(): Boolean {
      return areaOffset >= threshold
    }

    override fun toString(): String {
      return "Token(a=$areaOffset, r=$containerRect, w=$containerWidth, h=$containerHeight)"
    }
  }

  data class Config(
    val tag: Any = Master.NO_TAG,
    val delay: Int = 0,
    val threshold: Float = 0.65F,
    val preload: Boolean = false,
    val repeatMode: Int = Common.REPEAT_MODE_OFF,
    val callbacks: Set<Callback> = emptySet(),
    val controller: Controller? = null,
    val initialPlaybackInfo: PlaybackInfo? = null,
    val artworkHintListener: ArtworkHintListener? = null,
    val tokenUpdateListener: TokenUpdateListener? = null,
    val networkTypeChangeListener: NetworkTypeChangeListener? = null
  )

  override fun toString(): String {
    return "${super.toString()}, [$playable], [${token}]"
  }

  protected open fun updateToken(): Token {
    "Playback#updateToken $this".logDebug()
    tmpRect.setEmpty()
    if (!lifecycleState.isAtLeast(STARTED)) {
      return Token(config.threshold, -1F, tmpRect, container.width, container.height)
    }

    if (!container.isAttachedToWindow) {
      return Token(config.threshold, -1F, tmpRect, container.width, container.height)
    }

    if (!container.getGlobalVisibleRect(tmpRect)) {
      return Token(config.threshold, -1F, tmpRect, container.width, container.height)
    }

    val drawArea = with(Rect()) {
      container.getDrawingRect(this)
      container.clipBounds?.let {
        this.intersect(it)
      }
      width() * height()
    }

    val offset: Float =
      if (drawArea > 0) (tmpRect.width() * tmpRect.height()) / drawArea.toFloat()
      else 0F
    return Token(config.threshold, offset, tmpRect, container.width, container.height)
  }

  private val tmpRect = Rect()
  private val callbacks = ArrayDeque<Callback>()
  private val listeners = ArrayDeque<StateListener>()

  // Callbacks those will be setup when the Playback is added, and cleared when it is removed.
  private var controller: Controller? = null
  private var artworkHintListener: ArtworkHintListener? = null
  private var tokenUpdateListener: TokenUpdateListener? = null
  private var networkTypeChangeListener: NetworkTypeChangeListener? = null

  /**
   * Returns a usable renderer for the [playable], or `null` if no renderer is available or needed.
   *
   * @see [RendererProvider]
   * @see [RendererProvider.acquireRenderer]
   */
  internal open fun acquireRenderer(): Any? {
    val playable = this.playable
    requireNotNull(playable)
    val provider: RendererProvider = manager.findRendererProvider(playable)
    return provider.acquireRenderer(this, playable.media)
  }

  /**
   * Releases the renderer back to the Provider as it is no longer used by this [Playback]. Return
   * `true` if the renderer is null (so nothing need to be done) or the operation finishes
   * successfully; `false` otherwise.
   *
   * @see [RendererProvider]
   * @see [RendererProvider.releaseRenderer]
   */
  internal open fun releaseRenderer(renderer: Any?): Boolean {
    val playable = this.playable
    requireNotNull(playable)
    val provider: RendererProvider = manager.findRendererProvider(playable)
    return provider.releaseRenderer(this, playable.media, renderer)
  }

  /**
   * Return `true` if this Playback successfully attaches the renderer, `false` otherwise.
   */
  internal fun attachRenderer(renderer: Any?): Boolean {
    "Playback#attachRenderer $renderer $this".logDebug()
    return onAttachRenderer(renderer)
  }

  /**
   * Return `true` if this Playback successfully detaches the renderer, `false` otherwise.
   */
  internal fun detachRenderer(renderer: Any?): Boolean {
    "Playback#detachRenderer $renderer $this".logDebug()
    return onDetachRenderer(renderer)
  }

  /**
   * Return `true` to indicate that the Renderer is safely attached to container and
   * can be used by the Playable.
   */
  protected abstract fun onAttachRenderer(renderer: Any?): Boolean

  /**
   * Return `true` to indicate that the Renderer is safely detached from container and
   * Playable should not use it any longer. [RendererProvider] will then release the Renderer with
   * proper mechanism (eg: put it back to Pool for reuse).
   */
  protected abstract fun onDetachRenderer(renderer: Any?): Boolean

  internal fun onRendererAttached(renderer: Any?) {
    controller?.setupRenderer(this, renderer)
  }

  internal fun onRendererDetached(renderer: Any?) {
    controller?.teardownRenderer(this, renderer)
  }

  internal fun onAdded() {
    "Playback#onAdded $this".logDebug()
    playbackState = STATE_ADDED
    callbacks.forEach { it.onAdded(this) }
    controller = config.controller
    artworkHintListener = config.artworkHintListener
    tokenUpdateListener = config.tokenUpdateListener
    networkTypeChangeListener = config.networkTypeChangeListener
    playerParameters = networkTypeChangeListener?.onNetworkTypeChanged(manager.master.networkType)
        ?: playerParameters
    bucket.addContainer(this.container)
  }

  internal fun onRemoved() {
    "Playback#onRemoved $this".logDebug()
    playbackState = STATE_REMOVED
    bucket.removeContainer(this.container)
    controller = null
    tokenUpdateListener = null
    artworkHintListener = null
    networkTypeChangeListener = null
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
        this,
        playable?.isPlaying() == false,
        playbackInfo.resumePosition,
        playerState
    )
  }

  @CallSuper
  internal open fun onInActive() {
    "Playback#onInActive $this".logDebug()
    playbackState = STATE_INACTIVE
    artworkHintListener?.onArtworkHint(this, true, playbackInfo.resumePosition, playerState)
    playable?.teardownRenderer(this)
    callbacks.forEach { it.onInActive(this) }
  }

  @CallSuper
  internal open fun onPlay() {
    "Playback#onPlay $this".logDebug()
    container.keepScreenOn = true
    artworkHintListener?.onArtworkHint(
        this, playerState == STATE_ENDED, playbackInfo.resumePosition, playerState
    )
  }

  @CallSuper
  internal open fun onPause() {
    container.keepScreenOn = false
    "Playback#onPause $this".logDebug()
    artworkHintListener?.onArtworkHint(this, true, playbackInfo.resumePosition, playerState)
  }

  // Will be updated everytime 'onRefresh' is called.
  private var playbackToken: Token =
    Token(config.threshold, -1F, Rect(), 0, 0)

  internal val token: Token
    get() = playbackToken

  internal fun onRefresh() {
    "Playback#onRefresh $this".logDebug()
    playbackToken = updateToken()
    tokenUpdateListener?.onTokenUpdate(this, token)
    "Playback#onRefresh token updated -> $this".logDebug()
  }

  private var playbackState: Int = STATE_CREATED

  internal val isAttached: Boolean
    get() = playbackState >= STATE_ATTACHED

  internal val isActive: Boolean
    get() = playbackState >= STATE_ACTIVE

  internal var lifecycleState: State = State.INITIALIZED

  // Smaller value = higher priority. Priority 0 is the selected Playback. The higher priority, the
  // closer it is to the selected Playback.
  // Consider to prepare the underline Playable for high enough priority, and release it otherwise.
  // This value is updated by Group. Inactive Playback always has Int.MAX_VALUE priority.
  internal var playbackPriority: Int = Int.MAX_VALUE
    set(value) {
      val oldPriority = field
      field = value
      val newPriority = field
      "Playback#playbackPriority $oldPriority --> $newPriority, $this".logDebug()
      if (oldPriority == newPriority) return
      playable?.onPlaybackPriorityChanged(this, oldPriority, newPriority)
    }

  internal var playbackVolumeInfo: VolumeInfo = bucket.effectiveVolumeInfo(bucket.volumeInfo)
    set(value) {
      val from = field
      field = value
      val to = field
      "Playback#volumeInfo $from --> $to, $this".logDebug()
      playable?.onVolumeInfoChanged(this, from, to)
    }

  init {
    playbackVolumeInfo = bucket.effectiveVolumeInfo(bucket.volumeInfo)
  }

  var playable: Playable? = null
    internal set(value) {
      field = value
      if (value != null && config.initialPlaybackInfo != null) {
        value.playbackInfo = config.initialPlaybackInfo
      }
    }

  internal var playerParametersChangeListener: PlayerParametersChangeListener? = null

  internal fun compareWith(
    other: Playback,
    orientation: Int
  ): Int {
    "Playback#compareWith $this $other, $this".logDebug()
    val thisToken = this.token
    val thatToken = other.token

    var result = when (orientation) {
      VERTICAL -> CENTER_Y.compare(thisToken, thatToken)
      HORIZONTAL -> CENTER_X.compare(thisToken, thatToken)
      BOTH_AXIS -> max(
          CENTER_Y.compare(thisToken, thatToken), CENTER_X.compare(thisToken, thatToken)
      )
      NONE_AXIS -> max(
          CENTER_Y.compare(thisToken, thatToken), CENTER_X.compare(thisToken, thatToken)
      )
      else -> 0
    }

    if (result == 0) result = compareValues(thisToken.areaOffset, thatToken.areaOffset)
    return result
  }

  internal fun onNetworkTypeChanged(networkType: NetworkType) {
    this.playerParameters = networkTypeChangeListener?.onNetworkTypeChanged(networkType)
        ?: this.playerParameters
  }

  // Public APIs

  val tag = config.tag

  private val playerState: Int
    get() = playable?.playerState ?: STATE_IDLE

  val volumeInfo: VolumeInfo
    get() = playbackVolumeInfo

  private val playbackInfo: PlaybackInfo
    get() = playable?.playbackInfo ?: PlaybackInfo()

  val containerRect: Rect
    get() = token.containerRect

  var playerParameters: PlayerParameters = PlayerParameters.DEFAULT
    set(value) {
      val from = field
      field = value
      val to = field
      if (from != to) playerParametersChangeListener?.onPlayerParametersChanged(to)
    }

  internal fun addCallback(callback: Callback) {
    "Playback#addCallback $callback, $this".logDebug()
    this.callbacks.push(callback)
  }

  internal fun removeCallback(callback: Callback?) {
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
    manager.master.dispatcher.post {
      playable?.onUnbind(this) ?: manager.removePlayback(this)
    }
  }

  /**
   * Resets the [playable] to its original state.
   *
   * @param refresh If `true`, also refresh everything.
   */
  @JvmOverloads
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
      Player.STATE_IDLE -> {
      }
      Player.STATE_BUFFERING -> {
        listeners.forEach { it.onBuffering(this@Playback, playWhenReady) }
      }
      Player.STATE_READY -> {
        listeners.forEach {
          if (playWhenReady) it.onPlaying(this@Playback) else it.onPaused(this@Playback)
        }
      }
      Player.STATE_ENDED -> {
        listeners.forEach { it.onEnded(this@Playback) }
      }
    }
    val playable = this.playable
    artworkHintListener?.onArtworkHint(
        playback = this,
        shouldShow = if (playable != null) !playable.isPlaying() else true,
        position = playbackInfo.resumePosition,
        state = playerState
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
    fun onRendered(playback: Playback) = Unit

    /**
     * Called when buffering status of the playback is changed.
     *
     * @param playWhenReady true if the Video will start playing once buffered enough, false otherwise.
     */
    @JvmDefault
    fun onBuffering(
      playback: Playback,
      playWhenReady: Boolean
    ) = Unit // ExoPlayer state: 2

    /** Called when the Video starts playing */
    @JvmDefault
    fun onPlaying(playback: Playback) = Unit // ExoPlayer state: 3, play flag: true

    /** Called when the Video is paused */
    @JvmDefault
    fun onPaused(playback: Playback) = Unit // ExoPlayer state: 3, play flag: false

    /** Called when the Video finishes its playback */
    @JvmDefault
    fun onEnded(playback: Playback) = Unit // ExoPlayer state: 4

    @JvmDefault
    fun onVideoSizeChanged(
      playback: Playback,
      width: Int,
      height: Int,
      unAppliedRotationDegrees: Int,
      pixelWidthHeightRatio: Float
    ) = Unit

    @JvmDefault
    fun onError(
      playback: Playback,
      exception: Exception
    ) = Unit
  }

  interface Callback {

    @JvmDefault
    fun onActive(playback: Playback) = Unit

    @JvmDefault
    fun onInActive(playback: Playback) = Unit

    @JvmDefault
    fun onAdded(playback: Playback) = Unit

    @JvmDefault
    fun onRemoved(playback: Playback) = Unit

    @JvmDefault
    fun onAttached(playback: Playback) = Unit

    @JvmDefault
    fun onDetached(playback: Playback) = Unit
  }

  /**
   * Provides necessary information and callbacks to setup a manual controller for a [Playback].
   */
  interface Controller {
    /**
     * Returns `true` if the library can automatically pause the [Playback], `false` otherwise.
     *
     * If this method returns `true`:
     * - Once the user starts a [Playback], it will not be paused **until** its container is not
     * visible enough (controlled by [Playback.Config.threshold]), or user starts other Playback
     * (priority overridden).
     * - Once the user pauses a [Playback], it will not be played until the user manually resumes
     * it.
     * - Once the user interacts so that the [Playback]'s container is not visible enough, the
     * library will pause the it.
     * - Once the user interacts so that a paused [Playback]'s container is visible enough, the
     * library will: play it if it was not paused by the user, or pause it if it was paused by the
     * user before (this is equal to doing nothing).
     *
     * Default result is `true`.
     */
    @JvmDefault
    fun kohiiCanPause(): Boolean = true

    /**
     * Returns `true` to tell if the library can start a [Playback] automatically for the first time
     * , or `false` otherwise.
     *
     * If this method returns `true`: the library can start a [Playback] automatically if it was
     * never be started or paused by the user. Once the user pauses it manually, only user can
     * resume it, the library should never start/resume the [Playback] automatically again.
     *
     * Default result is `false`.
     */
    @JvmDefault
    fun kohiiCanStart(): Boolean = false

    /**
     * This method is called once the renderer of the [Playback] becomes available. Client should
     * use this callback to setup the manual controller mechanism for the renderer. For example:
     * provide a user interface for controlling the playback.
     *
     * @see [Playback.onRendererAttached]
     */
    @JvmDefault
    fun setupRenderer(playback: Playback, renderer: Any?) = Unit

    /**
     * This method is called once the renderer of the [Playback] becomes unavailable to it. Client
     * should use this callback to clean up any manual controller mechanism set before. Note that
     * the library also does some cleanup by itself to ensure the sanity of the renderer.
     *
     * @see [Playback.onRendererDetached]
     */
    @JvmDefault
    fun teardownRenderer(playback: Playback, renderer: Any?) = Unit
  }

  interface ArtworkHintListener {

    /**
     * @param position current position of the playback in milliseconds.
     */
    fun onArtworkHint(
      playback: Playback,
      shouldShow: Boolean,
      position: Long,
      state: Int
    )
  }

  interface TokenUpdateListener {

    fun onTokenUpdate(playback: Playback, token: Token)
  }

  interface NetworkTypeChangeListener {

    fun onNetworkTypeChanged(networkType: NetworkType): PlayerParameters
  }
}
