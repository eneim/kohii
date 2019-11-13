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

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.core.app.ComponentActivity
import androidx.core.net.toUri
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.ExoPlayer
import kohii.core.Binder.Options
import kohii.findActivity
import kohii.logDebug
import kohii.logWarn
import kohii.media.Media
import kohii.media.MediaItem
import kohii.media.PlaybackInfo
import kohii.v1.Kohii
import kohii.v1.PendingState
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.Delegates

class Master private constructor(context: Context) : PlayableManager, ComponentCallbacks2 {

  enum class MemoryMode {
    /**
     * In AUTO mode, Kohii will judge the preferred memory situation using [preferredMemoryMode] method.
     */
    AUTO,

    /**
     * In LOW mode, Kohii will always release resource of unselected Playables/Playbacks
     * (whose distance to selected ones are from 1).
     */
    LOW,

    /**
     * In NORMAL mode, Kohii will only reset the Playables/Playbacks whose distance to selected ones
     * are 1 (so 'next to' selected ones). Others will be released.
     */
    NORMAL,

    /**

    ▒▒▒▒▒▒▒▒▄▄▄▄▄▄▄▄▒▒▒▒▒▒▒▒
    ▒▒▒▒▒▄█▀▀░░░░░░▀▀█▄▒▒▒▒▒
    ▒▒▒▄█▀▄██▄░░░░░░░░▀█▄▒▒▒
    ▒▒█▀░▀░░▄▀░░░░▄▀▀▀▀░▀█▒▒
    ▒█▀░░░░███░░░░▄█▄░░░░▀█▒
    ▒█░░░░░░▀░░░░░▀█▀░░░░░█▒
    ▒█░░░░░░░░░░░░░░░░░░░░█▒
    ▒█░░██▄░░▀▀▀▀▄▄░░░░░░░█▒
    ▒▀█░█░█░░░▄▄▄▄▄░░░░░░█▀▒
    ▒▒▀█▀░▀▀▀▀░▄▄▄▀░░░░▄█▀▒▒
    ▒▒▒█░░░░░░▀█░░░░░▄█▀▒▒▒▒
    ▒▒▒█▄░░░░░▀█▄▄▄█▀▀▒▒▒▒▒▒
    ▒▒▒▒▀▀▀▀▀▀▀▒▒▒▒▒▒▒▒▒▒▒▒▒

    In BALANCED mode, the release behavior is the same with 'NORMAL' mode, but unselected Playables/Playbacks will not be reset.
     */
    BALANCED,

    /**
     * HIGH mode must be specified by client.
     *
     * In HIGH mode, any unselected Playables/Playbacks whose distance to selected ones is less
     * than 8 will be reset. Others will be released. This mode is memory-intensive and can be
     * used in many-videos-yet-low-memory-usage scenario like simple/short Videos.
     */
    HIGH,

    /**
     * "For the bravest only"
     *
     * INFINITE mode must be specified by client.
     *
     * In INFINITE mode, no unselected Playables/Playbacks will ever be released due to distance
     * change (though Kohii will release the resource once they are inactive).
     */
    INFINITE
  }

  companion object {

    private const val MSG_CLEANUP = 1
    private const val MSG_BIND_PLAYABLE = 2
    private const val MSG_RELEASE_PLAYABLE = 3
    private const val MSG_DESTROY_PLAYABLE = 4

    internal val NO_TAG = Any()

    @Volatile private var master: Master? = null

    @JvmStatic
    operator fun get(context: Context) = master ?: synchronized(Master::class.java) {
      master ?: Master(context).also { master = it }
    }

    @JvmStatic
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  private class Dispatcher(val master: Master) : Handler(Looper.getMainLooper()) {

    override fun sendMessageAtTime(
      msg: Message,
      uptimeMillis: Long
    ): Boolean {
      "Send msg: ${msg.what}, ${msg.obj}".logDebug("Kohii::Bug")
      return super.sendMessageAtTime(msg, uptimeMillis)
    }

    override fun handleMessage(msg: Message) {
      "Handle msg: ${msg.what}, ${msg.obj}".logWarn("Kohii::Bug")
      when {
        msg.what == MSG_CLEANUP -> {
          master.cleanupPendingPlayables()
        }
        msg.what == MSG_BIND_PLAYABLE -> {
          val container = msg.obj as ViewGroup
          container.doOnAttach {
            master.requests.remove(it)
                ?.onBind()
          }
        }
        msg.what == MSG_RELEASE_PLAYABLE -> {
          val playable = (msg.obj as Playable<*>)
          playable.bridge.release()
        }
        msg.what == MSG_DESTROY_PLAYABLE -> {
          val playable = msg.obj as Playable<*>
          val clearState = msg.arg1 == 0
          master.onTearDown(playable, clearState)
        }
      }
    }
  }

  val app = context.applicationContext as Application
  val kohii = Kohii[app]

  internal val groups = mutableSetOf<Group>()
  internal val playables = mutableMapOf<Playable<*>, Any /* Playable tag */>()

  // We want to keep the map of manual Playables even if the Activity is destroyed and recreated.
  // TODO when to remove entries of this map?
  internal val playablesStartedByClient by lazy(NONE) { ArraySet<Any /* Playable tag */>() }
  // TODO when to remove entries of this map?
  internal val playablesPendingStates by lazy(NONE) {
    ArrayMap<Any /* Playable tag */, PendingState>()
  }
  // TODO design a dedicated mechanism for it, considering paging to save in-memory space.
  // TODO when to remove entries of this map?
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()

  private var trimMemoryLevel: Int by Delegates.observable(
      initialValue = RunningAppProcessInfo().let {
        ActivityManager.getMyMemoryState(it)
        it.lastTrimLevel
      },
      onChange = { _, _, _ ->
        // TODO consider to force groups to trim memory usage if need.
      }
  )

  internal fun preferredMemoryMode(actual: MemoryMode): MemoryMode {
    if (actual !== MemoryMode.AUTO) return actual
    return if (trimMemoryLevel >= TRIM_MEMORY_RUNNING_CRITICAL) MemoryMode.LOW else MemoryMode.BALANCED
  }

  private fun registerInternal(
    activity: ComponentActivity,
    host: Any,
    managerLifecycleOwner: LifecycleOwner,
    memoryMode: MemoryMode = MemoryMode.AUTO
  ): Manager {
    val group = groups.find { it.activity === activity } ?: Group(this, activity).also {
      activity.lifecycle.addObserver(it)
    }

    return group.managers.find { it.lifecycleOwner === managerLifecycleOwner }
        ?: Manager(this, group, host, managerLifecycleOwner, memoryMode)
  }

  private val engines = mutableMapOf<Class<*>, Engine<*>>()
  private val requests = mutableMapOf<ViewGroup /* Container */, BindRequest<*>>()

  /**
   * @param container container is the [ViewGroup] that holds the Video. It should be an empty
   * ViewGroup, or a PlayerView itself. Note that View can be created from [android.app.Service] so
   * its Context is no need to be an [android.app.Activity]
   */
  internal fun <RENDERER : Any> bind(
    playable: Playable<RENDERER>,
    tag: Any,
    container: ViewGroup,
    options: Options,
    callback: ((Playback) -> Unit)? = null
  ) {
    // Remove any queued bind requests for the same container.
    dispatcher.removeMessages(MSG_BIND_PLAYABLE, container)
    // Remove any queued release for the same Playable, as we are binding it now.
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    // Keep track of which Playable will be bound to which Container.
    // Scenario: in RecyclerView, binding a Video in 'onBindViewHolder' will not immediately trigger the binding,
    // because we wait for the Container to be attached to the Window first. So if a Playable is registered to be bound,
    // but then another Playable is registered to the same Container, we need to kick the previous Playable.
    requests[container] = BindRequest(this, playable, container, tag, options, callback)
    if (playable.manager == null) playable.manager = this
    dispatcher.obtainMessage(MSG_BIND_PLAYABLE, container)
        .sendToTarget()
  }

  internal fun tearDown(
    playable: Playable<*>,
    clearState: Boolean
  ) {
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    dispatcher.obtainMessage(MSG_DESTROY_PLAYABLE, clearState.compareTo(true), -1, playable)
        .sendToTarget()
  }

  internal fun onTearDown(
    playable: Playable<*>,
    clearState: Boolean
  ) {
    check(playable.manager == null) {
      "Teardown $playable, found manager: ${playable.manager}"
    }
    check(playable.playback == null) {
      "Teardown $playable, found playback: ${playable.playback}"
    }
    playable.onPause()
    playable.onRelease()
    playables.remove(playable)
    if (clearState) {
      playbackInfoStore.remove(playable.tag)
      playablesStartedByClient.remove(playable.tag)
      playablesPendingStates.remove(playable.tag)
    }
  }

  internal fun trySavePlaybackInfo(playable: Playable<*>) {
    if (playable.tag === NO_TAG) return
    if (!playbackInfoStore.containsKey(playable.tag)) {
      playbackInfoStore[playable.tag] = playable.playbackInfo
    }
  }

  // If this method is called, it must be before any call to playable.bridge.prepare(flag)
  internal fun tryRestorePlaybackInfo(playable: Playable<*>) {
    if (playable.tag === NO_TAG) return
    val cache = playbackInfoStore.remove(playable.tag)
    // Only restoring playback state if there is cached state, and the player is not ready yet.
    if (cache != null && playable.playerState <= Player.STATE_IDLE /* TODO change to internal const */) {
      playable.playbackInfo = cache
    }
  }

  internal fun cleanupPendingPlayables() {
    playables.filter { it.key.manager === this }
        .keys.toMutableList()
        .onEach {
          require(it.playback == null) {
            "$it has manager: ${this@Master} but found Playback: ${it.playback}"
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
            dispatcher.removeMessages(MSG_BIND_PLAYABLE, it.key)
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
    if (groups.map { it.managers }.isEmpty() && playables.isEmpty() /* TODO double check this */) {
      kohii.cleanUp()
    }
  }

  internal fun preparePlayable(
    playable: Playable<*>,
    loadSource: Boolean = false
  ) {
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    playable.bridge.prepare(loadSource)
  }

  internal fun releasePlayable(playable: Playable<*>) {
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.obtainMessage(MSG_RELEASE_PLAYABLE, playable)
        .sendToTarget()
  }

  // Public APIs

  fun register(
    fragment: Fragment,
    memoryMode: MemoryMode = MemoryMode.LOW
  ): Manager {
    val (activity, lifecycleOwner) = fragment.requireActivity() to fragment.viewLifecycleOwner
    return registerInternal(activity, fragment, lifecycleOwner, memoryMode = memoryMode)
  }

  fun register(
    activity: ComponentActivity,
    memoryMode: MemoryMode = MemoryMode.AUTO
  ): Manager {
    return registerInternal(activity, activity, activity, memoryMode = memoryMode)
  }

  @ExoPlayer
  fun setUp(media: Media): Binder<PlayerView> {
    @Suppress("UNCHECKED_CAST")
    val engine: Engine<PlayerView> =
      engines.getOrPut(PlayerView::class.java) { PlayerViewEngine(this) } as Engine<PlayerView>
    return engine.setUp(media)
  }

  @ExoPlayer
  fun setUp(uri: Uri) = setUp(MediaItem(uri))

  @ExoPlayer
  fun setUp(url: String) = setUp(url.toUri())

  @Suppress("MemberVisibilityCanBePrivate", "unused")
  fun <RENDERER : Any> registerEngine(
    key: Class<RENDERER>,
    engine: Engine<RENDERER>
  ) {
    engines[key] = engine
  }

  @Suppress("unused")
  fun <RENDERER : Any> unregisterEngine(engine: Engine<RENDERER>) {
    engines.filter { it.value === engine }
        .keys.forEach { engines.remove(it) }
  }

  // Must be a request to play from Client. This method will set necessary flags and refresh all.
  fun play(playable: Playable<*>) {
    val controller = playable.playback?.config?.controller
    if (playable.tag !== NO_TAG && controller != null) {
      requireNotNull(playable.playback).also {
        if (it.token.shouldPrepare()) playable.onReady()
        playablesPendingStates[playable.tag] = Kohii.PENDING_PLAY
        if (!controller.kohiiCanPause()) playablesStartedByClient.add(playable.tag)
        it.manager.refresh()
      }
    }
  }

  // Must be a request to pause from Client. This method will set necessary flags and refresh all.
  fun pause(playable: Playable<*>) {
    val controller = playable.playback?.config?.controller
    if (playable.tag !== NO_TAG && controller != null) {
      playablesPendingStates[playable.tag] = Kohii.PENDING_PAUSE
      playablesStartedByClient.remove(playable.tag)
      requireNotNull(playable.playback).manager.refresh()
    }
  }

  fun stick(playback: Playback) {
    playback.manager.stick(playback.host)
    playback.manager.group.stick(playback.manager)
    playback.manager.refresh()
  }

  fun unstick(playback: Playback) {
    playback.manager.group.unstick(playback.manager)
    playback.manager.unstick(playback.host)
    playback.manager.refresh()
  }

  // Lock all resources.
  fun lock() {
    TODO()
  }

  fun unlock() {
    TODO()
  }

  // ComponentCallbacks2

  override fun onLowMemory() {
    // Do nothing
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    // Do nothing
  }

  override fun onTrimMemory(level: Int) {
    trimMemoryLevel = level
  }

  internal fun <RENDERER : Any> onBind(
    playable: Playable<RENDERER>,
    tag: Any,
    container: ViewGroup,
    options: Options,
    callback: ((Playback) -> Unit)? = null
  ) {
    // Cancel any pending release/destroy request. This Playable deserves to live a bit longer.
    dispatcher.removeMessages(MSG_RELEASE_PLAYABLE, playable)
    dispatcher.removeMessages(MSG_DESTROY_PLAYABLE, playable)
    playables[playable] = tag
    val host = groups.asSequence()
        .mapNotNull { it.findHostForContainer(container) }
        .firstOrNull()

    requireNotNull(host) { "No Manager and Host available for $container" }

    val createNew by lazy(NONE) {
      val config = Playback.Config(
          delay = options.delay,
          threshold = options.threshold,
          preload = options.preload,
          repeatMode = options.repeatMode,
          controller = options.controller,
          callbacks = options.callbacks
      )

      when {
        // Scenario: Playable accepts renderer of type PlayerView, and the container is an instance of a subclass of PlayerView.
        playable.rendererType.isAssignableFrom(container::class.java) -> {
          StaticViewRendererPlayback(host.manager, host, config, container)
        }
        View::class.java.isAssignableFrom(playable.rendererType) -> {
          DynamicViewRendererPlayback(host.manager, host, config, container)
        }
        Fragment::class.java.isAssignableFrom(playable.rendererType) -> {
          DynamicFragmentRendererPlayback(host.manager, host, config, container)
        }
        else -> {
          throw IllegalArgumentException("Unsupported Renderer type: ${playable.rendererType}")
        }
      }
    }

    val sameContainer = host.manager.playbacks[container]
    val samePlayable = host.manager.group.playbacks.find { playable.playback === it }

    val resolvedPlayback = if (sameContainer == null) { // Bind to new Container
      if (samePlayable == null) {
        // both sameContainer and samePlayable are null --> fresh binding
        playable.playback = createNew
        host.manager.addPlayback(createNew)
        createNew
      } else {
        // samePlayable is not null --> a bound Playable to be rebound to other/new Container
        // Action: create new Playback for new Container, make the new binding and remove old binding of
        // the 'samePlayable' Playback
        playable.playback = createNew
        samePlayable.manager.removePlayback(samePlayable)
        host.manager.addPlayback(createNew)
        createNew
      }
    } else {
      if (samePlayable == null) {
        // sameContainer is not null but samePlayable is null --> new Playable is bound to a bound Container
        // Action: create new Playback for current Container, make the new binding and remove old binding of
        // the 'sameContainer'
        playable.playback = createNew
        sameContainer.manager.removePlayback(sameContainer)
        host.manager.addPlayback(createNew)
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
          playable.playback = createNew
          sameContainer.manager.removePlayback(sameContainer)
          samePlayable.manager.removePlayback(samePlayable)
          host.manager.addPlayback(createNew)
          createNew
        }
      }
    }

    callback?.invoke(resolvedPlayback)
  }

  internal class BindRequest<RENDERER : Any>(
    val master: Master,
    val playable: Playable<RENDERER>,
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
