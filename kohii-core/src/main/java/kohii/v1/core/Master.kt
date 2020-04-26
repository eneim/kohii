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

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Build.VERSION
import android.os.Handler
import android.os.Handler.Callback
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.util.Util
import kohii.v1.core.Binder.Options
import kohii.v1.core.MemoryMode.AUTO
import kohii.v1.core.MemoryMode.BALANCED
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.core.Playback.Config
import kohii.v1.debugOnly
import kohii.v1.findActivity
import kohii.v1.internal.DynamicFragmentRendererPlayback
import kohii.v1.internal.DynamicViewRendererPlayback
import kohii.v1.internal.MasterNetworkCallback
import kohii.v1.internal.StaticViewRendererPlayback
import kohii.v1.logDebug
import kohii.v1.logInfo
import kohii.v1.logWarn
import kohii.v1.media.PlaybackInfo
import java.util.concurrent.atomic.AtomicReference
import kotlin.LazyThreadSafetyMode.NONE

class Master private constructor(context: Context) : PlayableManager {

  companion object {

    private const val MSG_CLEANUP = 1
    private const val MSG_BIND_PLAYABLE = 2
    private const val MSG_RELEASE_PLAYABLE = 3
    private const val MSG_DESTROY_PLAYABLE = 4

    internal val NO_TAG = Any()

    @Volatile private var master: Master? = null

    @JvmStatic
    operator fun get(context: Context) = master ?: synchronized(this) {
      master ?: Master(context).also { master = it }
    }
  }

  internal class Dispatcher(val master: Master) : Handler(Looper.getMainLooper(), Callback { msg ->
    when (msg.what) {
      MSG_CLEANUP -> {
        master.cleanupPendingPlayables()
        true
      }
      MSG_BIND_PLAYABLE -> {
        val container = msg.obj as ViewGroup
        debugOnly {
          val request = master.requests[container]
          if (request != null) {
            "Request bind: ${request.tag}, $container, ${request.playable}".logInfo()
          }
        }
        container.doOnAttach {
          master.requests.remove(it)
              ?.onBind()
        }
        true
      }
      MSG_RELEASE_PLAYABLE -> {
        val playable = (msg.obj as Playable)
        playable.onRelease()
        true
      }
      MSG_DESTROY_PLAYABLE -> {
        val playable = msg.obj as Playable
        val clearState = msg.arg1 == 0
        master.onTearDown(playable, clearState)
        true
      }
      else -> false
    }
  })

  val app = context.applicationContext as Application

  internal val engines = mutableMapOf<Class<*>, Engine<*>>()
  internal val groups = mutableSetOf<Group>()
  internal val requests = mutableMapOf<ViewGroup /* Container */, BindRequest>()
  internal val playables = mutableMapOf<Playable, Any /* Playable tag */>()

  // Memorize the tags which belongs to Playbacks that enable manual playbacks. On config change,
  // we may not get the binding of these tags yet, but we may need to play them.
  // TODO when to remove entries of this map?
  internal val plannedManualPlayables = arraySetOf<Any /* Playable tag */>()

  // Reference of the Playable started manually by the User. This Playable can be paused if
  // the controller allows the library to pause it.
  internal val manuallyStartedPlayable = AtomicReference<Playable>()

  // TODO when to remove entries of this map?
  internal val playablesPendingActions = arrayMapOf<Any /* Playable tag */, PlaybackAction>()

  // TODO design a dedicated mechanism for this store, considering paging to save in-memory space.
  // TODO when to remove entries of this map?
  // TODO LruStore (temporary, short term), SqLiteStore (eternal, manual clean up), etc?
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()

  internal var groupsMaxLifecycleState: State = DESTROYED

  private val componentCallbacks = object : ComponentCallbacks2 {
    override fun onLowMemory() = Unit
    override fun onConfigurationChanged(newConfig: Configuration) = Unit
    override fun onTrimMemory(level: Int) {
      trimMemoryLevel = level
    }
  }

  private val screenStateReceiver = lazy(NONE) {
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?
      ) {
        if (isInitialStickyBroadcast) return
        if (context != null && intent != null) {
          if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Master[context].pause(Scope.GLOBAL)
          } else if (intent.action == Intent.ACTION_USER_PRESENT) {
            Master[context].resume(Scope.GLOBAL)
          }
        }
      }
    }
  }

  private val networkActionReceiver = lazy(NONE) {
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?
      ) {
        if (isInitialStickyBroadcast) return
        if (context != null) {
          Master[context].onNetworkChanged()
        }
      }
    }
  }

  @SuppressLint("NewApi") // will only be used in proper API level.
  private val networkCallback = lazy(NONE) {
    MasterNetworkCallback(this)
  }

  private var networkType: Int = Util.getNetworkType(app)
    set(value) {
      val from = field
      field = value
      val to = field
      if (from == to) return
      playables.forEach { it.key.onNetworkTypeChanged(from, to) }
    }

  internal fun onNetworkChanged() {
    this.networkType = Util.getNetworkType(app)
  }

  internal var trimMemoryLevel: Int = RunningAppProcessInfo().let {
    ActivityManager.getMyMemoryState(it)
    it.lastTrimLevel
  }
    set(value) {
      val from = field
      field = value
      val to = field
      if (from != to) groups.forEach { it.onRefresh() }
    }

  internal fun preferredMemoryMode(actual: MemoryMode): MemoryMode {
    if (actual !== AUTO) return actual
    return if (trimMemoryLevel >= TRIM_MEMORY_RUNNING_CRITICAL) LOW else BALANCED
  }

  internal fun registerInternal(
    activity: FragmentActivity,
    host: Any,
    managerLifecycleOwner: LifecycleOwner,
    memoryMode: MemoryMode = AUTO,
    activeLifecycleState: State
  ): Manager {
    check(!activity.isDestroyed) {
      "Cannot register a destroyed Activity: $activity"
    }
    val group = groups.find { it.activity === activity } ?: Group(
        this, activity
    ).also {
      onGroupCreated(it)
      activity.lifecycle.addObserver(it)
    }

    return group.managers.find { it.lifecycleOwner === managerLifecycleOwner }
        ?: Manager(this, group, host, managerLifecycleOwner, memoryMode, activeLifecycleState)
            .also {
              group.onManagerCreated(it)
              managerLifecycleOwner.lifecycle.addObserver(it)
            }
  }

  /**
   * @param container container is the [ViewGroup] that holds the Video. It should be an empty
   * ViewGroup, or a PlayerView itself. Note that View can be created from [android.app.Service] so
   * its Context doesn't need to be an [android.app.Activity]
   */
  internal fun bind(
    playable: Playable,
    tag: Any,
    container: ViewGroup,
    options: Options,
    callback: ((Playback) -> Unit)? = null
  ) {
    "Request queue: $tag, $container, $playable".logDebug()
    // Remove any queued bind requests for the same container.
    dispatcher.removeMessages(MSG_BIND_PLAYABLE, container)
    // Remove any queued releasing for the same Playable, as we are binding it now.
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    // Remove any queued destruction for the same Playable, as we are binding it now.
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    // Keep track of which Playable will be bound to which Container.
    // Scenario: in RecyclerView, binding a Video in 'onBindViewHolder' will not immediately
    // trigger the binding, because we wait for the Container to be attached to the Window first.
    // So if a Playable is registered to be bound, but then another Playable is registered to the
    // same Container, we need to kick the previous Playable.
    val sameTag = requests.asSequence()
        .filter { it.value.tag !== NO_TAG }
        .firstOrNull { it.value.tag == tag }
        ?.key
    if (sameTag != null) requests.remove(sameTag)?.onRemoved()
    requests[container] = BindRequest(
        this, playable, container, tag, options, callback
    )
    // if (playable.manager == null) playable.manager = this
    dispatcher.obtainMessage(MSG_BIND_PLAYABLE, container)
        .sendToTarget()
  }

  internal fun tearDown(
    playable: Playable,
    clearState: Boolean
  ) {
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    dispatcher.obtainMessage(MSG_DESTROY_PLAYABLE, clearState.compareTo(true), -1, playable)
        .sendToTarget()
  }

  internal fun onTearDown(
    playable: Playable,
    clearState: Boolean
  ) {
    "Master#onTearDown: $playable, clear: $clearState".logDebug()
    check(playable.manager == null || playable.manager === this) {
      "Teardown $playable, found manager: ${playable.manager}"
    }
    check(playable.playback == null) {
      "Teardown $playable, found playback: ${playable.playback}"
    }
    playable.onPause()
    trySavePlaybackInfo(playable)
    releasePlayable(playable)
    playables.remove(playable)
    if (playable === manuallyStartedPlayable.get()) {
      manuallyStartedPlayable.set(null)
    }
    if (playables.isEmpty()) cleanUp()
  }

  internal fun trySavePlaybackInfo(playable: Playable) {
    "Master#trySavePlaybackInfo: $playable".logDebug()
    val key = if (playable.tag !== NO_TAG) {
      playable.tag
    } else {
      // if there is no available tag, we use the playback as info key, to allow state caching in
      // the same session.
      val playback = playable.playback
      if (playback == null || !playback.isAttached) return
      playback
    }

    if (!playbackInfoStore.containsKey(key)) {
      val info = playable.playbackInfo
      "Master#trySavePlaybackInfo: $info, $playable".logInfo()
      playbackInfoStore[key] = info
    }
  }

  // If this method is called, it must be before any call to playable.bridge.prepare(flag)
  internal fun tryRestorePlaybackInfo(playable: Playable) {
    "Master#tryRestorePlaybackInfo: $playable".logDebug()
    val cache = if (playable.tag !== NO_TAG) {
      playbackInfoStore.remove(playable.tag)
    } else {
      val key = playable.playback ?: return
      playbackInfoStore.remove(key)
    }

    "Master#tryRestorePlaybackInfo: $cache, $playable".logInfo()
    // Only restoring playback state if there is cached state, and the player is not ready yet.
    if (cache != null && playable.playerState <= Common.STATE_IDLE) {
      playable.playbackInfo = cache
    }
  }

  // [Draft] return true if this [Master] wants to handle this step by itself, false otherwise.
  internal fun onPlaybackInActive(playable: Playable, playback: Playback): Boolean {
    "Master#onPlaybackInActive: $playback".logDebug()
    return manuallyStartedPlayable.get() === playable && playable.isPlaying()
  }

  internal fun onPlaybackDetached(playback: Playback) {
    "Master#onPlaybackDetached: $playback".logDebug()
    playbackInfoStore.remove(playback)
  }

  internal fun notifyPlaybackChanged(playable: Playable, from: Playback?, to: Playback?) {
    if (playable.tag !== NO_TAG) {
      for (group in groups) {
        group.notifyPlaybackChanged(playable, from, to)
      }
    }
  }

  internal fun cleanupPendingPlayables() {
    playables.filter { it.key.manager === this }
        .keys.toMutableList()
        .apply {
          val manuallyStartedPlayable = manuallyStartedPlayable.get()
          if (manuallyStartedPlayable != null && manuallyStartedPlayable.isPlaying()) {
            minusAssign(manuallyStartedPlayable)
          }
        }
        .onEach { playable ->
          require(playable.playback == null) {
            "$playable has manager: $this but found Playback: ${playable.playback}"
          }
          playable.manager = null
          tearDown(playable, true)
        }
        .clear()
  }

  internal val dispatcher = Dispatcher(this)

  internal fun onGroupLifecycleStateChanged() {
    groupsMaxLifecycleState = groups.map { it.activity.lifecycle.currentState }.max() ?: DESTROYED
  }

  internal fun onGroupCreated(group: Group) {
    if (groups.add(group)) dispatcher.sendEmptyMessage(MSG_CLEANUP)
  }

  internal fun onGroupDestroyed(group: Group) {
    if (groups.remove(group)) {
      requests.filter { it.key.context.findActivity() === group.activity }
          .forEach {
            dispatcher.removeMessages(MSG_BIND_PLAYABLE, it.key)
            it.value.playable.playback = null
            requests.remove(it.key)?.onRemoved()
          }
    }
    if (groups.isEmpty()) {
      dispatcher.removeMessages(MSG_CLEANUP)
      // The last activity is destroyed but not a config change.
      if (!group.activity.isChangingConfigurations) {
        manuallyStartedPlayable.get()?.manager = null
      }
    }
  }

  // Called when Manager is added (created)/removed (destroyed) to/from Group
  internal fun onGroupUpdated(group: Group) {
    requests.values.filter {
      val bucket = it.bucket
      return@filter bucket != null && bucket.manager.group === group
          && bucket.manager.lifecycleOwner.lifecycle.currentState < CREATED
    }
        .forEach {
          it.playable.playback = null
          requests.remove(it.container)?.onRemoved()
        }

    // If no Manager is online, cleanup stuffs
    if (groups.flatMap { it.managers }.isEmpty() && playables.isEmpty()) {
      cleanUp()
    }
  }

  internal fun onFirstManagerCreated(group: Group) {
    if (groups.flatMap { it.managers }.isEmpty()) {
      app.registerComponentCallbacks(componentCallbacks)
      app.registerReceiver(screenStateReceiver.value, IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_USER_PRESENT)
      })
      if (VERSION.SDK_INT >= 24 /* VERSION_CODES.N */) {
        val networkManager = ContextCompat.getSystemService(app, ConnectivityManager::class.java)
        networkManager?.registerDefaultNetworkCallback(networkCallback.value)
      } else {
        @Suppress("DEPRECATION")
        app.registerReceiver(
            networkActionReceiver.value, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
      }
    }
    engines.forEach { it.value.inject(group) }
  }

  @Suppress("UNUSED_PARAMETER")
  internal fun onLastManagerDestroyed(group: Group) {
    if (groups.flatMap { it.managers }.isEmpty()) {
      if (networkCallback.isInitialized() && VERSION.SDK_INT >= 24 /* VERSION_CODES.N */) {
        val networkManager = ContextCompat.getSystemService(app, ConnectivityManager::class.java)
        networkManager?.unregisterNetworkCallback(networkCallback.value)
      } else if (networkActionReceiver.isInitialized()) {
        app.unregisterReceiver(networkActionReceiver.value)
      }
      if (screenStateReceiver.isInitialized()) {
        app.unregisterReceiver(screenStateReceiver.value)
      }
      app.unregisterComponentCallbacks(componentCallbacks)
    }
  }

  private fun cleanUp() {
    engines.forEach { it.value.cleanUp() }
  }

  internal fun preparePlayable(
    playable: Playable,
    loadSource: Boolean = false
  ) {
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    playable.onPrepare(loadSource)
  }

  internal fun releasePlayable(playable: Playable) {
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.obtainMessage(MSG_RELEASE_PLAYABLE, playable)
        .sendToTarget()
  }

  // Must be a request to play from Client. This method will set necessary flags and refresh all.
  internal fun play(playable: Playable) {
    val tag = playable.tag
    if (tag == NO_TAG) return
    if (plannedManualPlayables.contains(tag)) {
      requireNotNull(playable.playback).also {
        if (!requireNotNull(it.config.controller).kohiiCanPause()) {
          manuallyStartedPlayable.set(playable)
        }
        playablesPendingActions[tag] = Common.PLAY
        it.manager.refresh()
      }
    }
  }

  // Must be a request to pause from Client. This method will set necessary flags and refresh all.
  internal fun pause(playable: Playable) {
    val tag = playable.tag
    if (tag == NO_TAG) return
    val controller = playable.playback?.config?.controller
    if (controller != null) {
      playablesPendingActions[tag] = Common.PAUSE
      manuallyStartedPlayable.set(null)
      requireNotNull(playable.playback).manager.refresh()
    }
  }

  /**
   * Manually pause an object in a specific scope. After Playbacks of a Scope is paused by this method,
   * only another call to [resume(scope, receiver)] with same or higher priority Scope will resume it.
   * For example:
   * - Pausing a Playback with Scope.PLAYBACK --> a call to resume(Scope.PLAYBACK, playback) or
   * resume(Scope.BUCKET, playback) will also resume it.
   * - Pausing all Playback in a Manager by using Scope.MANAGER --> a call to resume(bucket, Scope.BUCKET)
   * will not resume anything, including Playbacks of containers inside to that Bucket.
   *
   * To be able to change the scope of Playbacks need to be paused, client must:
   * - Resume all Playbacks of the same or higher priority Scope.
   * - Call this method to pause Playbacks of expected scope.
   */
  internal fun pause(
    scope: Scope = Scope.GLOBAL,
    receiver: Any? = null
  ) {
    when (scope) {
      Scope.GLOBAL ->
        this.groups.forEach {
          this.pause(Scope.GROUP, it)
        }
      Scope.GROUP ->
        when (receiver) {
          is Group -> receiver.lock = true // will lock all managers
          is Manager -> this.pause(Scope.GROUP, receiver.group)
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a Manager or a Group"
          )
        }
      Scope.MANAGER -> {
        require(receiver is Manager) { "Receiver for scope $scope must be a Manager" }
        receiver.lock = true
      }
      Scope.BUCKET ->
        when (receiver) {
          is Bucket -> receiver.lock = true
          is Playback -> this.pause(Scope.BUCKET, receiver.bucket)
          else -> {
            val bucket = groups.asSequence()
                .flatMap { it.managers.asSequence() }
                .flatMap { it.buckets.asSequence() }
                .firstOrNull { it.root === receiver }
            if (bucket != null) this.pause(Scope.BUCKET, bucket)
          }
        }
      Scope.PLAYBACK -> {
        require(receiver is Playback) { "Receiver for scope $scope must be a Playback" }
        receiver.playable?.let { pause(it) }
      }
    }
  }

  /**
   * Manually pause an object in a specific scope. After Playbacks of a Scope is paused by this method,
   * only another call to [resume(scope, receiver)] with same or higher priority Scope will resume it.
   * For example:
   * - Pausing a Playback with Scope.PLAYBACK --> a call to resume(Scope.PLAYBACK, playback) or
   * resume(Scope.BUCKET, playback) will also resume it.
   * - Pausing all Playback in a Manager by using Scope.MANAGER --> a call to resume(bucket, Scope.BUCKET)
   * will not resume anything, including Playbacks of containers inside to that Bucket.
   *
   * To be able to change the scope of Playbacks need to be paused, client must:
   * - Resume all Playbacks of the same or higher priority Scope.
   * - Call this method to pause Playbacks of expected scope.
   */
  internal fun resume(
    scope: Scope = Scope.GLOBAL,
    receiver: Any? = null
  ) {
    when {
      scope === Scope.GLOBAL ->
        this.groups.forEach {
          this.resume(Scope.GROUP, it)
        }
      scope === Scope.GROUP ->
        when (receiver) {
          is Group -> {
            receiver.lock = false
            receiver.managers
                .forEach { this.resume(Scope.MANAGER, it) }
          }
          is Manager -> this.resume(Scope.GROUP, receiver.group)
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a Manager or a Group"
          )
        }
      scope === Scope.MANAGER ->
        (receiver as? Manager)?.let {
          it.lock = false
          it.buckets.forEach { bucket -> this.resume(Scope.BUCKET, bucket) }
        } ?: throw IllegalArgumentException("Receiver for scope $scope must be a Manager")
      scope === Scope.BUCKET ->
        when (receiver) {
          is Bucket -> {
            receiver.lock = false
            receiver.manager.refresh()
          }
          is Playback -> this.resume(Scope.BUCKET, receiver.bucket)
          else -> {
            // Find the TargetHost whose host is this receiver
            val bucket = groups.asSequence()
                .flatMap { it.managers.asSequence() }
                .flatMap { it.buckets.asSequence() }
                .firstOrNull { it.root === receiver }

            if (bucket != null) this.resume(Scope.BUCKET, bucket)
          }
        }
      scope === Scope.PLAYBACK -> {
        require(receiver is Playback) { "Receiver for scope $scope must be a Playback" }
        receiver.manager.refresh()
      }
    }
  }

  fun registerEngine(engine: Engine<*>) {
    engines.put(engine.playableCreator.rendererType, engine)
        ?.cleanUp()
    groups.forEach { engine.inject(it) }
  }

  internal inline fun onBind(
    playable: Playable,
    tag: Any,
    manager: Manager,
    container: ViewGroup,
    noinline callback: ((Playback) -> Unit)? = null,
    crossinline createNewPlayback: () -> Playback
  ) {
    // Cancel any pending release/destroy request. This Playable needs to live a bit longer.
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)

    playables[playable] = tag

    val playbackForSameContainer = manager.playbacks[container]
    val playbackForSamePlayable = playable.playback

    val resolvedPlayback = //
      if (playbackForSameContainer == null) { // Bind to new Container
        if (playbackForSamePlayable == null) {
          // both sameContainer and samePlayable are null --> fresh binding
          val newPlayback = createNewPlayback()
          playable.playback = newPlayback
          manager.addPlayback(newPlayback)
          newPlayback
        } else {
          // samePlayable is not null --> a bound Playable to be rebound to other/new Container
          // Action: create new Playback for new Container, make the new binding and remove old binding of
          // the 'samePlayable' Playback
          playbackForSamePlayable.manager.removePlayback(playbackForSamePlayable)
          dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
          val newPlayback = createNewPlayback()
          playable.playback = newPlayback
          manager.addPlayback(newPlayback)
          newPlayback
        }
      } else {
        if (playbackForSamePlayable == null) {
          // sameContainer is not null but samePlayable is null --> new Playable is bound to a bound Container
          // Action: create new Playback for current Container, make the new binding and remove old binding of
          // the 'sameContainer'
          playbackForSameContainer.manager.removePlayback(playbackForSameContainer)
          dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
          val newPlayback = createNewPlayback()
          playable.playback = newPlayback
          manager.addPlayback(newPlayback)
          newPlayback
        } else {
          // both sameContainer and samePlayable are not null --> a bound Playable to be rebound to a bound Container
          if (playbackForSameContainer === playbackForSamePlayable) {
            // Nothing to do
            playbackForSamePlayable
          } else {
            // Scenario: rebind a bound Playable from one Container to other Container that is being bound.
            // Action: remove both 'sameContainer' and 'samePlayable', create new one for the Container.
            // to the Container
            playbackForSameContainer.manager.removePlayback(playbackForSameContainer)
            playbackForSamePlayable.manager.removePlayback(playbackForSamePlayable)
            dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
            val newPlayback = createNewPlayback()
            playable.playback = newPlayback
            manager.addPlayback(newPlayback)
            newPlayback
          }
        }
      }

    callback?.invoke(resolvedPlayback)
  }

  internal class BindRequest(
    val master: Master,
    val playable: Playable,
    val container: ViewGroup,
    val tag: Any,
    val options: Options,
    val callback: ((Playback) -> Unit)?
  ) {

    // used by RecyclerViewBucket to 'assume' that it will hold this container
    // It is recommended to use Engine#cancel to easily remove a queued request from cache.
    internal var bucket: Bucket? = null

    internal fun onBind() {
      val bucket = master.groups.asSequence()
          .mapNotNull { it.findBucketForContainer(container) }
          .firstOrNull()

      requireNotNull(bucket) { "No Manager and Bucket available for $container" }

      master.onBind(playable, tag, bucket.manager, container, callback, createNewPlayback@{
        val config = Config(
            tag = options.tag,
            delay = options.delay,
            threshold = options.threshold,
            preload = options.preload,
            repeatMode = options.repeatMode,
            controller = options.controller,
            artworkHintListener = options.artworkHintListener,
            tokenUpdateListener = options.tokenUpdateListener,
            callbacks = options.callbacks
        )

        return@createNewPlayback when {
          // Scenario: Playable accepts renderer of type PlayerView, and
          // the container is an instance of PlayerView or its subtype.
          playable.config.rendererType.isAssignableFrom(container.javaClass) -> {
            StaticViewRendererPlayback(bucket.manager, bucket, container, config)
          }
          View::class.java.isAssignableFrom(playable.config.rendererType) -> {
            DynamicViewRendererPlayback(bucket.manager, bucket, container, config)
          }
          Fragment::class.java.isAssignableFrom(playable.config.rendererType) -> {
            DynamicFragmentRendererPlayback(bucket.manager, bucket, container, config)
          }
          else -> {
            throw IllegalArgumentException(
                "Unsupported Renderer type: ${playable.config.rendererType}"
            )
          }
        }
      })
      "Request bound: $tag, $container, $playable".logInfo()
    }

    internal fun onRemoved() {
      "Request removed: $tag, $container, $playable".logWarn()
      options.artworkHintListener = null
      options.controller = null
      options.callbacks.clear()
    }

    override fun toString(): String {
      return "R: $tag, $container"
    }
  }

  // Public APIs

  fun lock() {
    this.pause(Scope.GLOBAL)
  }

  fun unlock() {
    this.resume(Scope.GLOBAL)
  }
}
