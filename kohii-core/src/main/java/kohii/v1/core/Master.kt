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

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.NetworkTypeObserver
import kohii.v1.core.Binder.Options
import kohii.v1.core.MemoryMode.AUTO
import kohii.v1.core.MemoryMode.BALANCED
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.core.Playback.Config
import kohii.v1.core.Scope.BUCKET
import kohii.v1.core.Scope.GLOBAL
import kohii.v1.core.Scope.GROUP
import kohii.v1.core.Scope.MANAGER
import kohii.v1.core.Scope.PLAYBACK
import kohii.v1.findActivity
import kohii.v1.internal.DynamicFragmentRendererPlayback
import kohii.v1.internal.DynamicViewRendererPlayback
import kohii.v1.internal.StaticViewRendererPlayback
import kohii.v1.logDebug
import kohii.v1.logInfo
import kohii.v1.logWarn
import kohii.v1.media.PlaybackInfo
import kohii.v1.utils.Capsule
import java.util.concurrent.atomic.AtomicReference

class Master private constructor(context: Context) : PlayableManager {

  companion object {

    internal const val MSG_CLEANUP = 1
    internal const val MSG_BIND_PLAYABLE = 2
    internal const val MSG_RELEASE_PLAYABLE = 3
    internal const val MSG_DESTROY_PLAYABLE = 4

    internal val NO_TAG = Any()

    private val capsule: Capsule<Master, Context> = Capsule(::Master)

    @JvmStatic
    operator fun get(context: Context) = capsule.get(context.applicationContext)
  }

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
  internal val playablesPendingActions = arrayMapOf<Any /* Playable tag */, Boolean>()

  // TODO design a dedicated mechanism for this store, considering paging to save in-memory space.
  // TODO when to remove entries of this map?
  // TODO LruStore (temporary, short term), SqLiteStore (eternal, manual clean up), etc?
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()

  private var systemLock: Boolean = false
    set(value) {
      field = value
      groups.forEach(Group::onRefresh)
    }

  internal var lock: Boolean = false
    get() = field || systemLock
    set(value) {
      field = value
      groups.forEach { it.lock = lock }
    }

  internal var groupsMaxLifecycleState: State = DESTROYED

  private val componentCallbacks = object : ComponentCallbacks2 {
    override fun onLowMemory() = Unit
    override fun onConfigurationChanged(newConfig: Configuration) = Unit
    override fun onTrimMemory(level: Int) {
      trimMemoryLevel = level
    }
  }

  @C.NetworkType
  internal var networkType: NetworkType = C.NETWORK_TYPE_UNKNOWN
    set(value) {
      val from = field
      field = value
      val to = field
      if (from == to) return
      playables.asSequence()
          .filter { (playable: Playable, _) -> playable.playback?.isActive == true }
          .forEach { (playable: Playable, _) -> playable.onNetworkTypeChanged(from, to) }
    }

  private val networkTypeChangedListener = NetworkTypeObserver.Listener { networkType ->
    this@Master.networkType = networkType
  }

  internal var trimMemoryLevel: Int = RunningAppProcessInfo().let {
    ActivityManager.getMyMemoryState(it)
    it.lastTrimLevel
  }
    set(value) {
      val from = field
      field = value
      val to = field
      if (from != to) groups.forEach(Group::onRefresh)
    }

  init {
    NetworkTypeObserver.getInstance(app).register(networkTypeChangedListener)
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStart(owner: LifecycleOwner) {
        systemLock = false
      }

      override fun onStop(owner: LifecycleOwner) {
        systemLock = true
      }
    })
  }

  internal fun preferredMemoryMode(actual: MemoryMode): MemoryMode =
    if (trimMemoryLevel >= TRIM_MEMORY_RUNNING_LOW) {
      LOW
    } else {
      if (actual !== AUTO) actual else BALANCED
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
    val group = groups.find { it.activity === activity } ?: Group(this, activity).also {
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
    "Master#bind tag=$tag, playable=$playable, container=$container, options=$options".logInfo()
    // Remove any queued binding requests for the same container.
    dispatcher.removeMessages(MSG_BIND_PLAYABLE, container)
    // Remove any queued releasing request for the same Playable, since we are binding it now.
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    // Remove any queued destroying request for the same Playable, since we are binding it now.
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    // Keep track of which Playable will be bound to which Container.
    // Scenario: in RecyclerView, binding a Video in 'onBindViewHolder' will not immediately
    // trigger the binding, because we wait for the Container to be attached to the Window first.
    // So if a Playable is registered to be bound, but then another Playable is registered to the
    // same Container, we need to kick the previous Playable.
    val requestForSameTag = requests.asSequence()
        .filter { it.value.tag !== NO_TAG }
        .firstOrNull { it.value.tag == tag }
        ?.key
    if (requestForSameTag != null) requests.remove(requestForSameTag)?.onRemoved()
    requests[container] = BindRequest(this, playable, container, tag, options, callback)
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
    if (cache != null && playable.playerState <= Player.STATE_IDLE) {
      playable.playbackInfo = cache
    }
  }

  // [Draft] return false if this [Master] wants to handle this step by itself, true to release.
  internal fun releasePlaybackOnInActive(playback: Playback): Boolean {
    "Master#releasePlaybackOnInActive: $playback".logDebug()
    if (!playback.config.releaseOnInActive) return false
    val playable: Playable? = manuallyStartedPlayable.get()
    return !(playable === playback.playable && playable?.isPlaying() == true)
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

  internal val dispatcher = MasterDispatcher(this)

  internal fun onGroupLifecycleStateChanged() {
    groupsMaxLifecycleState = groups.maxOfOrNull { it.activity.lifecycle.currentState } ?: DESTROYED
  }

  internal fun onGroupCreated(group: Group) {
    if (groups.add(group)) dispatcher.sendEmptyMessage(MSG_CLEANUP)
  }

  internal fun onGroupDestroyed(group: Group) {
    if (groups.remove(group)) {
      requests.filter { (container, _) -> container.context.findActivity() === group.activity }
          .forEach { (container, request) ->
            dispatcher.removeMessages(MSG_BIND_PLAYABLE, container)
            request.playable.playback = null
            requests.remove(container)?.onRemoved()
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
      return@filter bucket != null && bucket.manager.group === group &&
          bucket.manager.lifecycleOwner.lifecycle.currentState < CREATED
    }
        .forEach {
          it.playable.playback = null
          requests.remove(it.container)?.onRemoved()
        }

    // If no Manager is online, cleanup stuffs
    if (groups.flatMap(Group::managers).isEmpty() && playables.isEmpty()) {
      cleanUp()
    }
  }

  internal fun onFirstManagerCreated(group: Group) {
    if (groups.flatMap(Group::managers).isEmpty()) {
      app.registerComponentCallbacks(componentCallbacks)
    }
    engines.forEach { it.value.inject(group) }
  }

  @Suppress("UNUSED_PARAMETER")
  internal fun onLastManagerDestroyed(group: Group) {
    if (groups.flatMap(Group::managers).isEmpty()) {
      app.unregisterComponentCallbacks(componentCallbacks)
    }
  }

  private fun cleanUp() {
    engines.forEach { it.value.cleanUp() }
  }

  internal fun findBucketForContainer(container: ViewGroup): Bucket? {
    return groups.asSequence()
        .mapNotNull { it.findBucketForContainer(container) }
        .firstOrNull()
  }

  internal fun preparePlayable(
    playable: Playable,
    loadSource: Boolean = false
  ) {
    "Master#preparePlayable playable=$playable, loadSource=$loadSource".logInfo()
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    playable.onPrepare(loadSource)
  }

  internal fun releasePlayable(playable: Playable) {
    "Master#releasePlayable playable=$playable".logInfo()
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.obtainMessage(MSG_RELEASE_PLAYABLE, playable)
        .sendToTarget()
  }

  internal fun removeBinding(container: Any) {
    requests.remove(container)
        ?.also { it.playable.playback = null }
        ?.onRemoved()

    groups.asSequence()
        .flatMap { it.managers.asSequence() }
        .map { it.playbacks[container] }
        .firstOrNull()
        ?.also { playback ->
          playback.manager.removePlayback(playback)
        }
  }

  // Must be a request to play from Client. This method will set necessary flags and refresh all.
  // Note: the last manual start wins. Which means that it will pause any other playing items and
  // try to start the new one.
  internal fun play(playable: Playable) {
    val tag = playable.tag
    if (tag == NO_TAG) return
    if (plannedManualPlayables.contains(tag)) {
      requireNotNull(playable.playback).also {
        // TODO(eneim): rethink this to support off-screen manual playback/kohiiCanPause().
        /* if (!requireNotNull(it.config.controller).kohiiCanPause()) {
          manuallyStartedPlayable.set(playable)
        } */
        manuallyStartedPlayable.set(playable)
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
   * Lock an object in a specific scope. Any playing Playbacks of the same scope will be paused.
   * After Playbacks of a Scope is paused by this method, only another call to [unlock] by the same
   * or higher priority Scope will resume it.
   * For example:
   * - Lock a Playback with Scope.PLAYBACK --> a call to unlock(playback, Scope.PLAYBACK) or
   * unlock(playback, Scope.BUCKET) will also unlock it.
   * - Lock all Playbacks in a Manager by using Scope.MANAGER --> a call to unlock(bucket, Scope.BUCKET)
   * will not resume anything, including Playbacks of containers inside that Bucket.
   *
   * To change the lock scope of a Playback, the client must:
   * - Unlock all Playbacks of the same or higher priority scope.
   * - Call this method to lock Playbacks by the expected scope.
   */
  internal fun lock(
    target: Any? = null,
    scope: Scope = GLOBAL
  ) {
    when (scope) {
      GLOBAL -> this.lock = true
      GROUP -> {
        when (target) {
          is Group -> target.lock = true // will lock all managers
          is Manager -> lock(target.group, GROUP)
          is FragmentActivity -> {
            val group = groups.firstOrNull { it.activity === target }
            if (group != null) lock(group, GROUP)
          }
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a Manager or a Group"
          )
        }
      }
      MANAGER -> {
        when (target) {
          is Manager -> target.lock = true
          is Bucket -> lock(target.manager, MANAGER)
          is Playback -> lock(target.manager, MANAGER)
          else -> throw IllegalArgumentException("Target for scope $scope must be a Manager")
        }
      }
      BUCKET -> {
        when (target) {
          is Bucket -> target.lock = true
          is Playback -> lock(target.bucket, BUCKET)
          else -> {
            val bucket = groups.asSequence()
                .flatMap { it.managers.asSequence() }
                .flatMap { it.buckets.asSequence() }
                .firstOrNull { it.root === target }
            if (bucket != null) lock(bucket, BUCKET)
          }
        }
      }
      PLAYBACK -> {
        if (target is Playback) target.lock = true
        else throw IllegalArgumentException("Target for scope $scope must be a Playback")
      }
    }
  }

  /**
   * Unlock an object in a specific scope. It can only unlock those Playbacks that was locked by the
   * same or lower scope.
   *
   * @see lock
   * @see Scope
   */
  internal fun unlock(
    target: Any? = null,
    scope: Scope = GLOBAL
  ) {
    when (scope) {
      GLOBAL -> this.lock = false
      GROUP -> {
        when (target) {
          is Group -> if (!target.master.lock) target.lock = false // -> unlock managers internally
          is Manager -> unlock(target.group, GROUP)
          is FragmentActivity -> {
            val group = groups.firstOrNull { it.activity === target }
            if (group != null) unlock(group, GROUP)
          }
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a Manager or a Group"
          )
        }
      }
      MANAGER -> {
        when (target) {
          is Manager -> if (!target.group.lock) target.lock = false
          is Bucket -> unlock(target.manager, MANAGER)
          is Playback -> unlock(target.manager, MANAGER)
          else -> throw IllegalArgumentException("Target for scope $scope must be a Manager")
        }
      }
      BUCKET -> {
        when (target) {
          is Bucket -> if (!target.manager.lock) target.lock = false
          is Playback -> unlock(target.bucket, BUCKET)
          else -> {
            // Find the Bucket whose root is this receiver
            val bucket = groups.asSequence()
                .flatMap { it.managers.asSequence() }
                .flatMap { it.buckets.asSequence() }
                .firstOrNull { it.root === target }

            if (bucket != null) unlock(bucket, BUCKET)
          }
        }
      }
      PLAYBACK -> {
        if (target is Playback) {
          if (!target.bucket.lock) target.lock = false
        } else {
          throw IllegalArgumentException("Target for scope $scope must be a Playback")
        }
      }
    }
  }

  fun registerEngine(engine: Engine<*>) {
    engines.put(engine.playableCreator.rendererType, engine)
        ?.cleanUp()
    groups.forEach(engine::inject)
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
      val bucket = master.findBucketForContainer(container)

      requireNotNull(bucket) { "No Manager and Bucket available for $container" }

      master.onBind(playable, tag, bucket.manager, container, callback) createNewPlayback@{
        val config = Config(
            tag = options.tag,
            delay = options.delay,
            threshold = options.threshold,
            preload = options.preload,
            repeatMode = options.repeatMode,
            controller = options.controller,
            initialPlaybackInfo = options.initialPlaybackInfo,
            artworkHintListener = options.artworkHintListener,
            tokenUpdateListener = options.tokenUpdateListener,
            networkTypeChangeListener = options.networkTypeChangeListener,
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
      }
      "Request bound: $tag, $container, $playable".logInfo()
    }

    internal fun onRemoved() {
      "Request removed: $tag, $container, $playable".logWarn()
      options.controller = null
      options.artworkHintListener = null
      options.networkTypeChangeListener = null
      options.tokenUpdateListener = null
      options.callbacks.clear()
    }

    override fun toString(): String {
      return "R: $tag, $container"
    }
  }

  // Public APIs

  /**
   * Globally lock the behavior.
   */
  fun lock() = lock(scope = GLOBAL)

  /**
   * Globally unlock the behavior.
   */
  fun unlock() = unlock(scope = GLOBAL)
}
