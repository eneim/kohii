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
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import kohii.v1.PendingState
import kohii.v1.core.Binder.Options
import kohii.v1.core.MemoryMode.AUTO
import kohii.v1.core.MemoryMode.BALANCED
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.core.Playback.Config
import kohii.v1.findActivity
import kohii.v1.internal.DynamicFragmentRendererPlayback
import kohii.v1.internal.DynamicViewRendererPlayback
import kohii.v1.internal.StaticViewRendererPlayback
import kohii.v1.logInfo
import kohii.v1.media.PlaybackInfo
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.Delegates

class Master private constructor(context: Context) : PlayableManager {

  companion object {

    private const val MSG_CLEANUP = 1
    private const val MSG_BIND_PLAYABLE = 2
    private const val MSG_RELEASE_PLAYABLE = 3
    private const val MSG_DESTROY_PLAYABLE = 4

    internal val NO_TAG = Any()

    @Volatile private var master: Master? = null

    @JvmStatic
    operator fun get(context: Context) = master ?: synchronized(Master::javaClass) {
      master ?: Master(context).also { master = it }
    }
  }

  private class Dispatcher(val master: Master) : Handler(Looper.getMainLooper()) {

    override fun handleMessage(msg: Message) {
      when (msg.what) {
        MSG_CLEANUP -> {
          master.cleanupPendingPlayables()
        }
        MSG_BIND_PLAYABLE -> {
          val container = msg.obj as ViewGroup
          container.doOnAttach {
            master.requests.remove(it)
                ?.onBind()
          }
        }
        MSG_RELEASE_PLAYABLE -> {
          val playable = (msg.obj as Playable)
          playable.onRelease()
        }
        MSG_DESTROY_PLAYABLE -> {
          val playable = msg.obj as Playable
          val clearState = msg.arg1 == 0
          master.onTearDown(playable, clearState)
        }
      }
    }
  }

  internal val app = context.applicationContext as Application

  internal val groups = mutableSetOf<Group>()
  internal val playables = mutableMapOf<Playable, Any /* Playable tag */>()

  // We want to keep the map of manual Playables even if the Activity is destroyed and recreated.
  // TODO when to remove entries of this map?
  internal val playablesStartedByClient by lazy(NONE) { ArraySet<Any /* Playable tag */>() }
  // TODO when to remove entries of this map?
  internal val playablesPendingStates by lazy(NONE) {
    ArrayMap<Any /* Playable tag */, PendingState>()
  }
  // TODO design a dedicated mechanism for it, considering paging to save in-memory space.
  // TODO when to remove entries of this map?
  // TODO LruStore (temporary, short term), SqLiteStore (eternal, manual clean up), etc?
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()

  private val componentCallbacks by lazy(NONE) {
    object : ComponentCallbacks2 {
      override fun onLowMemory() {
        // do nothing
      }

      override fun onConfigurationChanged(newConfig: Configuration) {
        // do nothing
      }

      override fun onTrimMemory(level: Int) {
        trimMemoryLevel = level
      }
    }
  }

  internal var trimMemoryLevel: Int by Delegates.observable(
      initialValue = RunningAppProcessInfo().let {
        ActivityManager.getMyMemoryState(it)
        it.lastTrimLevel
      },
      onChange = { _, from, to ->
        if (from != to) groups.forEach { it.onRefresh() }
      }
  )

  internal fun preferredMemoryMode(actual: MemoryMode): MemoryMode {
    if (actual !== AUTO) return actual
    return if (trimMemoryLevel >= TRIM_MEMORY_RUNNING_CRITICAL) LOW else BALANCED
  }

  internal fun registerInternal(
    activity: FragmentActivity,
    host: Any,
    managerLifecycleOwner: LifecycleOwner,
    memoryMode: MemoryMode = AUTO
  ): Manager {
    val group = groups.find { it.activity === activity } ?: Group(
        this, activity
    ).also {
      activity.lifecycle.addObserver(it)
    }

    return group.managers.find { it.lifecycleOwner === managerLifecycleOwner }
        ?: Manager(
            this, group, host, managerLifecycleOwner, memoryMode
        )
  }

  private val engines = mutableMapOf<Class<*>, Engine<*>>()
  private val requests = mutableMapOf<ViewGroup /* Container */, BindRequest>()

  /**
   * @param container container is the [ViewGroup] that holds the Video. It should be an empty
   * ViewGroup, or a PlayerView itself. Note that View can be created from [android.app.Service] so
   * its Context is no need to be an [android.app.Activity]
   */
  internal fun bind(
    playable: Playable,
    tag: Any,
    container: ViewGroup,
    options: Options,
    callback: ((Playback) -> Unit)? = null
  ) {
    // Remove any queued bind requests for the same container.
    dispatcher.removeMessages(MSG_BIND_PLAYABLE, container)
    // Remove any queued releasing for the same Playable, as we are binding it now.
    dispatcher.removeMessages(
        MSG_RELEASE_PLAYABLE, playable
    )
    // Remove any queued destruction for the same Playable, as we are binding it now.
    dispatcher.removeMessages(
        MSG_DESTROY_PLAYABLE, playable
    )
    // Keep track of which Playable will be bound to which Container.
    // Scenario: in RecyclerView, binding a Video in 'onBindViewHolder' will not immediately trigger the binding,
    // because we wait for the Container to be attached to the Window first. So if a Playable is registered to be bound,
    // but then another Playable is registered to the same Container, we need to kick the previous Playable.
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
    dispatcher.removeMessages(
        MSG_DESTROY_PLAYABLE, playable
    )
    dispatcher.obtainMessage(MSG_DESTROY_PLAYABLE, clearState.compareTo(true), -1, playable)
        .sendToTarget()
  }

  internal fun onTearDown(
    playable: Playable,
    clearState: Boolean
  ) {
    check(playable.manager == null) {
      "Teardown $playable, found manager: ${playable.manager}"
    }
    check(playable.playback == null) {
      "Teardown $playable, found playback: ${playable.playback}"
    }
    playable.onPause()
    trySavePlaybackInfo(playable)
    releasePlayable(playable)
    playables.remove(playable)
    if (clearState) {
      playbackInfoStore.remove(playable.tag)
      playablesStartedByClient.remove(playable.tag)
      playablesPendingStates.remove(playable.tag)
    }

    if (playables.isEmpty()) cleanUp()
  }

  internal fun trySavePlaybackInfo(playable: Playable) {
    if (playable.tag === NO_TAG) return
    if (!playbackInfoStore.containsKey(playable.tag)) {
      val info = playable.playbackInfo
      "Master#trySavePlaybackInfo $info, $playable".logInfo()
      playbackInfoStore[playable.tag] = info
    }
  }

  // If this method is called, it must be before any call to playable.bridge.prepare(flag)
  internal fun tryRestorePlaybackInfo(playable: Playable) {
    if (playable.tag === NO_TAG) return
    val cache = playbackInfoStore.remove(playable.tag)
    // Only restoring playback state if there is cached state, and the player is not ready yet.
    if (cache != null && playable.playerState <= Common.STATE_IDLE /* TODO change to internal const */) {
      "Master#tryRestorePlaybackInfo $cache, $playable".logInfo()
      playable.playbackInfo = cache
    }
  }

  internal fun cleanupPendingPlayables() {
    playables.filter { it.key.manager === this }
        .keys.toMutableList()
        .onEach {
          require(it.playback == null) {
            "$it has manager: $this but found Playback: ${it.playback}"
          }
          it.manager = null
          tearDown(it, true)
        }
        .clear()
  }

  private val dispatcher = Dispatcher(this)

  internal fun onGroupCreated(group: Group) {
    groups.add(group)
    dispatcher.sendEmptyMessage(MSG_CLEANUP)
  }

  internal fun onGroupDestroyed(group: Group) {
    if (groups.remove(group)) {
      requests.filter { it.key.context.findActivity() === group.activity }
          .forEach {
            dispatcher.removeMessages(
                MSG_BIND_PLAYABLE, it.key
            )
            it.value.playable.playback = null
            requests.remove(it.key)
          }
    }
    if (groups.isEmpty()) {
      dispatcher.removeMessages(MSG_CLEANUP)
    }
  }

  // Called when Manager is added/removed to/from Group
  @Suppress("UNUSED_PARAMETER")
  internal fun onGroupUpdated(group: Group) {
    // If no Manager is online, cleanup stuffs
    if (groups.map { it.managers }.isEmpty() && playables.isEmpty()) {
      cleanUp()
    }
  }

  internal fun onFirstManagerCreated(group: Group) {
    if (groups.isEmpty()) app.registerComponentCallbacks(componentCallbacks)
    engines.forEach { it.value.inject(group) }
  }

  internal fun onLastManagerDestroyed(group: Group) {
    if (groups.map { it.managers }.isEmpty()) app.unregisterComponentCallbacks(componentCallbacks)
  }

  private fun cleanUp() {
    engines.forEach { it.value.cleanUp() }
  }

  internal fun preparePlayable(
    playable: Playable,
    loadSource: Boolean = false
  ) {
    dispatcher.removeMessages(
        MSG_RELEASE_PLAYABLE, playable
    )
    playable.onPrepare(loadSource)
  }

  internal fun releasePlayable(playable: Playable) {
    dispatcher.removeMessages(
        MSG_RELEASE_PLAYABLE, playable
    )
    dispatcher.obtainMessage(MSG_RELEASE_PLAYABLE, playable)
        .sendToTarget()
  }

  // Must be a request to play from Client. This method will set necessary flags and refresh all.
  internal fun play(playable: Playable) {
    val controller = playable.playback?.config?.controller
    if (playable.tag !== NO_TAG && controller != null) {
      requireNotNull(playable.playback).also {
        if (it.token.shouldPrepare()) playable.onReady()
        playablesPendingStates[playable.tag] = Common.PENDING_PLAY
        if (!controller.kohiiCanPause()) playablesStartedByClient.add(playable.tag)
        it.manager.refresh()
      }
    }
  }

  // Must be a request to pause from Client. This method will set necessary flags and refresh all.
  internal fun pause(playable: Playable) {
    val controller = playable.playback?.config?.controller
    if (playable.tag !== NO_TAG && controller != null) {
      playablesPendingStates[playable.tag] = Common.PENDING_PAUSE
      playablesStartedByClient.remove(playable.tag)
      requireNotNull(playable.playback).manager.refresh()
    }
  }

  // Public APIs

  // target == null --> lock all
  // TODO design 'lock' policy.
  /**
   * Will lock all
   */
  fun lock() {
    groups.forEach { it.lock = true }
  }

  // target == null --> unlock all
  fun unlock() {
    groups.forEach { it.lock = false }
  }

  internal fun registerEngine(engine: Engine<*>) {
    engines.put(engine.playableCreator.rendererType, engine)
        ?.cleanUp()
    groups.forEach { engine.inject(it) }
  }

  internal fun onBind(
    playable: Playable,
    tag: Any,
    container: ViewGroup,
    options: Options,
    callback: ((Playback) -> Unit)? = null
  ) {
    // Cancel any pending release/destroy request. This Playable deserves to live a bit longer.
    dispatcher.removeMessages(
        MSG_RELEASE_PLAYABLE, playable
    )
    dispatcher.removeMessages(
        MSG_DESTROY_PLAYABLE, playable
    )
    playables[playable] = tag
    val bucket = groups.asSequence()
        .mapNotNull { it.findBucketForContainer(container) }
        .firstOrNull()

    requireNotNull(bucket) { "No Manager and Bucket available for $container" }

    val createNew by lazy(NONE) {
      val config = Config(
          tag = options.tag,
          delay = options.delay,
          threshold = options.threshold,
          preload = options.preload,
          repeatMode = options.repeatMode,
          // TODO 2019/11/18 temporarily disable manual playback. Will revise the logic.
          // controller = options.controller,
          callbacks = options.callbacks
      )

      when {
        // Scenario: Playable accepts renderer of type PlayerView, and
        // the container is an instance PlayerView or its subtype.
        playable.config.rendererType.isAssignableFrom(container.javaClass) -> {
          StaticViewRendererPlayback(
              bucket.manager, bucket, config, container
          )
        }
        View::class.java.isAssignableFrom(playable.config.rendererType) -> {
          DynamicViewRendererPlayback(
              bucket.manager, bucket, config, container
          )
        }
        Fragment::class.java.isAssignableFrom(playable.config.rendererType) -> {
          DynamicFragmentRendererPlayback(
              bucket.manager, bucket, config, container
          )
        }
        else -> {
          throw IllegalArgumentException(
              "Unsupported Renderer type: ${playable.config.rendererType}"
          )
        }
      }
    }

    val sameContainer = bucket.manager.playbacks[container]
    val samePlayable = playable.playback

    val resolvedPlayback = //
      if (sameContainer == null) { // Bind to new Container
        if (samePlayable == null) {
          // both sameContainer and samePlayable are null --> fresh binding
          playable.playback = createNew
          bucket.manager.addPlayback(createNew)
          createNew
        } else {
          // samePlayable is not null --> a bound Playable to be rebound to other/new Container
          // Action: create new Playback for new Container, make the new binding and remove old binding of
          // the 'samePlayable' Playback
          samePlayable.manager.removePlayback(samePlayable)
          dispatcher.removeMessages(
              MSG_DESTROY_PLAYABLE, playable
          )
          playable.playback = createNew
          bucket.manager.addPlayback(createNew)
          createNew
        }
      } else {
        if (samePlayable == null) {
          // sameContainer is not null but samePlayable is null --> new Playable is bound to a bound Container
          // Action: create new Playback for current Container, make the new binding and remove old binding of
          // the 'sameContainer'
          sameContainer.manager.removePlayback(sameContainer)
          dispatcher.removeMessages(
              MSG_DESTROY_PLAYABLE, playable
          )
          playable.playback = createNew
          bucket.manager.addPlayback(createNew)
          createNew
        } else {
          // both sameContainer and samePlayable are not null --> a bound Playable to be rebound to a bound Container
          if (sameContainer === samePlayable) {
            // Nothing to do
            samePlayable
          } else {
            // Scenario: rebind a bound Playable from one Container to other Container that is being bound.
            // Action: remove both 'sameContainer' and 'samePlayable', create new one for the Container.
            // to the Container
            sameContainer.manager.removePlayback(sameContainer)
            samePlayable.manager.removePlayback(samePlayable)
            dispatcher.removeMessages(
                MSG_DESTROY_PLAYABLE, playable
            )
            playable.playback = createNew
            bucket.manager.addPlayback(createNew)
            createNew
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

    internal fun onBind() {
      master.onBind(playable, tag, container, options, callback)
    }
  }
}
