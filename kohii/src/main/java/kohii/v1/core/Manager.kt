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

import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.view.ViewCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.Lifecycle.Event.ON_ANY
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.v1.core.Master.MemoryMode
import kohii.v1.core.Master.MemoryMode.LOW
import kohii.v1.core.Scope.GLOBAL
import kohii.v1.core.Scope.GROUP
import kohii.v1.core.Scope.HOST
import kohii.v1.core.Scope.MANAGER
import kohii.v1.core.Scope.PLAYBACK
import kohii.v1.media.VolumeInfo
import kohii.v1.partitionToMutableSets
import java.util.ArrayDeque
import kotlin.properties.Delegates

class Manager(
  val master: Master,
  internal val group: Group,
  internal val host: Any,
  internal val lifecycleOwner: LifecycleOwner,
  internal val memoryMode: MemoryMode = LOW
) : PlayableManager, LifecycleObserver, Comparable<Manager> {

  companion object {
    internal fun compareAndCheck(
      left: Prioritized,
      right: Prioritized
    ): Int {
      val ltr = left.compareTo(right)
      val rtl = right.compareTo(left)
      check(ltr + rtl == 0) {
        "Sum of comparison result of 2 directions must be 0, get ${ltr + rtl}."
      }

      return ltr
    }
  }

  // Use as both Queue and Stack.
  // - When adding new Host, we add it to tail of the Queue.
  // - When promoting a Host as sticky, we push it to head of the Queue.
  // - When demoting a Host from sticky, we just poll the head.
  private val hosts = ArrayDeque<Host>(4 /* less than default minimum of ArrayDeque */)
  // Up to one Host can be sticky at a time.
  private var stickyHost by Delegates.observable<Host?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        // Move 'to' from hosts.
        if (to != null /* set new sticky Host */) {
          hosts.push(to) // Push it to head.
        } else { // 'to' is null then 'from' must be nonnull. Consider to remove it from head.
          if (hosts.peek() === from) hosts.pop()
        }
      }
  )

  internal val playbacks = mutableMapOf<Any /* container */, Playback>()

  internal var sticky: Boolean = false

  internal var volumeInfoUpdater: VolumeInfo by Delegates.observable(
      initialValue = VolumeInfo(),
      onChange = { _, from, to ->
        if (from == to) return@observable
        // Update VolumeInfo of all Hosts. This operation will then callback to this #applyVolumeInfo
        hosts.forEach { it.volumeInfoUpdater = to }
      }
  )

  internal val volumeInfo: VolumeInfo
    get() = volumeInfoUpdater

  init {
    volumeInfoUpdater = group.volumeInfo
    group.onManagerCreated(this)
    lifecycleOwner.lifecycle.addObserver(this)
  }

  override fun compareTo(other: Manager): Int {
    return if (other.host !is Prioritized) {
      if (this.host is Prioritized) 1 else 0
    } else {
      if (this.host is Prioritized) {
        compareAndCheck(this.host, other.host)
      } else -1
    }
  }

  @OnLifecycleEvent(ON_ANY)
  internal fun onAnyEvent(owner: LifecycleOwner) {
    playbacks.forEach { it.value.lifecycleState = owner.lifecycle.currentState }
  }

  @OnLifecycleEvent(ON_CREATE)
  internal fun onCreate() {
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    playbacks.values.toMutableList()
        .onEach { removePlayback(it) /* also modify 'playbacks' content */ }
        .clear()
    stickyHost = null // will pop current sticky Host from the Stack
    hosts.toMutableList()
        .onEach { removeHost(it.root) }
        .clear()
    owner.lifecycle.removeObserver(this)
    group.onManagerDestroyed(this)
  }

  @OnLifecycleEvent(ON_START)
  internal fun onStart() {
    refresh() // This will also update active/inactive Playbacks accordingly.
  }

  @OnLifecycleEvent(ON_STOP)
  internal fun onStop() {
    playbacks.forEach { if (it.value.isActive) onPlaybackInActive(it.value) }
    refresh()
  }

  internal fun findPlayableForContainer(container: ViewGroup): Playable? {
    val playback = playbacks[container]
    return master.playables.keys.find { it.playback === playback }
  }

  internal fun findHostForContainer(container: ViewGroup): Host? {
    require(ViewCompat.isAttachedToWindow(container))
    return hosts.find { it.accepts(container) }
  }

  internal fun onContainerAttachedToWindow(container: Any?) {
    val playback = playbacks[container]
    if (playback != null) {
      onPlaybackAttached(playback)
      onPlaybackActive(playback)
      refresh()
    }
  }

  internal fun onContainerDetachedFromWindow(container: Any?) {
    // A detached Container can be re-attached later (in case of RecyclerView)
    val playback = playbacks[container]
    if (playback != null) {
      if (playback.isAttached) {
        if (playback.isActive) onPlaybackInActive(playback)
        onPlaybackDetached(playback)
      }
      refresh()
    }
  }

  internal fun onContainerLayoutChanged(container: Any?) {
    val playback = playbacks[container]
    if (playback != null) refresh()
  }

  private fun addHost(view: View) {
    val existing = hosts.find { it.root === view }
    require(existing == null) { "This host ${existing?.root} is attached already." }
    val host = Host[this@Manager, view]
    if (hosts.add(host)) {
      host.onAdded()
      view.doOnAttach { v ->
        host.onAttached()
        v.doOnDetach {
          detachHost(v)
        } // In case the View is detached immediately ...
      }
    }
  }

  private fun detachHost(view: View) {
    hosts.firstOrNull { it.root === view }
        ?.onDetached()
  }

  private fun removeHost(view: View) {
    hosts.firstOrNull { it.root === view && hosts.remove(it) }
        ?.onRemoved()
  }

  internal fun refresh() {
    group.onRefresh()
  }

  private fun refreshPlaybackStates(): Pair<MutableSet<Playback> /* Active */, MutableSet<Playback> /* InActive */> {
    val toActive = playbacks.filterValues { !it.isActive && it.token.shouldPrepare() }
        .values
    val toInActive = playbacks.filterValues { it.isActive && !it.token.shouldPrepare() }
        .values

    toActive.forEach { onPlaybackActive(it) }
    toInActive.forEach { onPlaybackInActive(it) }

    return playbacks.entries.filter { it.value.isAttached }
        .partitionToMutableSets(
            predicate = { it.value.isActive },
            transform = { it.value }
        )
  }

  internal fun splitPlaybacks(): Pair<Set<Playback> /* toPlay */, Set<Playback> /* toPause */> {
    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    val toPlay = ArraySet<Playback>()

    val hostToPlaybacks = playbacks.values.groupBy { it.host } // -> Map<Host, List<Playback>
    hosts.asSequence()
        .filter { !hostToPlaybacks[it].isNullOrEmpty() }
        .map {
          val all = hostToPlaybacks.getValue(it)
          val candidates = all.filter { playback -> it.allowToPlay(playback) }
          it to candidates
        }
        .map { (host, candidates) ->
          host.selectToPlay(candidates)
        }
        .find { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          activePlaybacks.removeAll(it)
        }

    activePlaybacks.addAll(inactivePlaybacks)
    return toPlay to activePlaybacks
  }

  internal fun addPlayback(playback: Playback) {
    val prev = playbacks.put(playback.container, playback)
    require(prev == null)
    playback.lifecycleState = lifecycleOwner.lifecycle.currentState
    playback.onAdded()
  }

  internal fun removePlayback(playback: Playback) {
    if (playbacks.remove(playback.container) === playback) {
      if (playback.isAttached) {
        if (playback.isActive) onPlaybackInActive(playback)
        onPlaybackDetached(playback)
      }
      playback.onRemoved()
      refresh()
    }
  }

  internal fun onRemoveContainer(container: Any) {
    playbacks[container]?.let { removePlayback(it) }
  }

  private fun onPlaybackAttached(playback: Playback) {
    playback.onAttached()
  }

  private fun onPlaybackDetached(playback: Playback) {
    playback.onDetached()
  }

  private fun onPlaybackActive(playback: Playback) {
    playback.onActive()
  }

  private fun onPlaybackInActive(playback: Playback) {
    playback.onInActive()
  }

  internal fun requestRenderer(
    playback: Playback,
    playable: Playable
  ): Any? {
    val renderer = group.findRendererProvider(playable)
        .acquireRenderer(playback, playable.media)
    return if (playback.attachRenderer(renderer)) renderer else null
  }

  internal fun releaseRenderer(
    playback: Playback,
    playable: Playable
  ) {
    val renderer = playable.renderer
    if (playback.detachRenderer(renderer)) {
      group.findRendererProvider(playable)
          .releaseRenderer(playback, playable.media, renderer)
    }
  }

  // Public APIs

  fun registerRendererProvider(
    type: Class<*>,
    provider: RendererProvider
  ) {
    group.registerRendererProvider(type, provider)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun unregisterRendererProvider(provider: RendererProvider) {
    group.unregisterRendererProvider(provider)
  }

  fun attach(vararg views: View): Manager {
    views.forEach { this.addHost(it) }
    return this
  }

  fun attach(vararg hosts: Host): Manager {
    hosts.forEach { host ->
      require(host.manager === this)
      val existing = this.hosts.find { it.root === host.root }
      require(existing == null) { "This host ${existing?.root} is attached already." }
      if (this.hosts.add(host)) host.onAdded()
    }
    return this
  }

  @Suppress("unused")
  fun detach(vararg views: View): Manager {
    views.forEach { this.removeHost(it) }
    return this
  }

  internal fun stick(host: Host) {
    this.stickyHost = host
  }

  // Null host --> unstick all current sticky hosts
  internal fun unstick(host: Host?) {
    if (host == null || this.stickyHost === host) {
      this.stickyHost = null
    }
  }

  internal fun updateHostVolumeInfo(
    host: Host,
    volumeInfo: VolumeInfo
  ) {
    playbacks.forEach { if (it.value.host === host) it.value.volumeInfoUpdater = volumeInfo }
  }

  /**
   * Apply a specific [VolumeInfo] to all Playbacks in a [Scope].
   * - The smaller a scope's priority is, the wider applicable range it will be.
   * - Applying new [VolumeInfo] to smaller [Scope] will change [VolumeInfo] of Playbacks in that [Scope].
   * - If the [Scope] is from [Scope.HOST], any new [Playback] added to that [Host] will be configured
   * with the updated [VolumeInfo].
   *
   * @param target is the container to apply new [VolumeInfo] to. This must be set together with the [Scope].
   * For example, if client wants to apply the [VolumeInfo] to [Scope.PLAYBACK], the receiver must be the [Playback]
   * to apply to. If client wants to apply to [Scope.HOST], the receiver must be either the [Playback] inside that [Host],
   * or the root object of a [Host].
   */
  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    target: Any,
    scope: Scope
  ) {
    when (scope) {
      PLAYBACK -> {
        require(target is Playback) { "Expected Playback, found ${target.javaClass.canonicalName}" }
        target.volumeInfoUpdater = volumeInfo
      }
      HOST -> {
        when (target) {
          is Host -> target.volumeInfoUpdater = volumeInfo
          is Playback -> target.host.volumeInfoUpdater = volumeInfo
          // If neither Playback nor Host, must be the root View of the Host.
          else -> {
            requireNotNull(hosts.find { it.root === target }) {
              "$target is not a root of any Host."
            }
                .volumeInfoUpdater = volumeInfo
          }
        }
      }
      MANAGER -> {
        this.volumeInfoUpdater = volumeInfo
      }
      GROUP -> {
        this.group.volumeInfoUpdater = volumeInfo
      }
      GLOBAL -> {
        this.master.groups.forEach { it.volumeInfoUpdater = volumeInfo }
      }
    }
  }

  interface OnSelectionListener {

    // Called when some Playbacks under this Manager are selected.
    fun onSelection(selection: Collection<Playback>)
  }
}
