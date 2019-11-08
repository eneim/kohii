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

package kohii.dev

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
import kohii.dev.Master.MemoryMode
import kohii.dev.Master.MemoryMode.LOW
import kohii.partitionToArrayLists
import kotlin.properties.Delegates

class Manager(
  internal val master: Master,
  internal val group: Group,
  internal val host: Any,
  internal val lifecycleOwner: LifecycleOwner,
  internal val memoryMode: MemoryMode = LOW
) : PlayableManager, LifecycleObserver {

  private val hosts = linkedSetOf<Host<*>>()

  // All Playbacks, including attached/detached ones.
  internal val playbacks = mutableMapOf<Any /* container */, Playback<*>>()

  @OnLifecycleEvent(ON_ANY)
  internal fun onAnyEvent(owner: LifecycleOwner) {
    playbacks.forEach { it.value.lifecycleState = owner.lifecycle.currentState }
  }

  @OnLifecycleEvent(ON_CREATE)
  internal fun onCreate() {
    group.onManagerCreated(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    playbacks.values.toMutableList()
        .onEach { removePlayback(it) /* also modify 'playbacks' content */ }
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
    playbacks.filterValues { it.isActive }
        .forEach { onPlaybackInActive(it.value) }
    refresh()
  }

  internal fun findPlayableForContainer(container: ViewGroup): Playable<*>? {
    val playback = playbacks[container]
    return master.playables.keys.firstOrNull { it.playback === playback }
  }

  internal fun findHostForContainer(container: ViewGroup): Host<*>? {
    require(ViewCompat.isAttachedToWindow(container))
    return hosts.firstOrNull { it.accepts(container) }
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
    // A detached Container can be re-attached (in case of RecyclerView)
    val playback = playbacks[container]
    if (playback != null) {
      if (playback.isAttached) { // Attached
        if (playback.isActive) onPlaybackInActive(playback)
        onPlaybackDetached(playback)
      }
      refresh()
    }
  }

  internal fun onContainerLayoutChanged(container: Any?) {
    val playback = playbacks[container]
    if (playback != null && playback.host.allowToPlay(playback)) {
      refresh()
    }
  }

  private fun attachHost(view: View) {
    val existing = hosts.find { it.root === view }
    require(existing == null) { "This host ${existing?.root} is attached already." }
    view.doOnAttach { v ->
      val host = Host[this@Manager, v]
      if (hosts.add(host)) host.onAdded()
    }
    view.doOnDetach { v -> detachHost(v) }
  }

  private fun detachHost(view: View) {
    hosts.filter { it.root === view }
        .forEach { if (hosts.remove(it)) it.onRemoved() }
  }

  internal fun refresh() {
    group.onRefresh()
  }

  private fun refreshPlaybackStates(): Pair<MutableCollection<Playback<*>> /* Active */, MutableCollection<Playback<*>> /* InActive */> {
    val toActive = playbacks.filterValues { !it.isActive && it.token.shouldPrepare() }
        .values
    val toInActive = playbacks.filterValues { it.isActive && !it.token.shouldPrepare() }
        .values

    toActive.forEach { onPlaybackActive(it) }
    toInActive.forEach { onPlaybackInActive(it) }

    return playbacks.asSequence()
        .filter { it.value.isAttached }
        .partitionToArrayLists(
            predicate = { it.value.isActive },
            transform = { it.value }
        )
  }

  internal fun splitPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    val toPlay = ArraySet<Playback<*>>()

    val hostToPlaybacks = playbacks.values.groupBy { it.host }
    hosts.asSequence()
        .filter { !hostToPlaybacks[it].isNullOrEmpty() }
        .map {
          val all = hostToPlaybacks.getValue(it)
          val candidates = all.filter { playback -> it.allowToPlay(playback) }
          Triple(it, candidates, all)
        }
        .map { (host, candidates, all) ->
          host.selectToPlay(candidates, all)
        }
        .firstOrNull { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          activePlaybacks.removeAll(it)
        }

    activePlaybacks.addAll(inactivePlaybacks)
    return toPlay to activePlaybacks
  }

  internal fun addPlayback(playback: Playback<*>) {
    val prev = playbacks.put(playback.container, playback)
    require(prev == null) {
      "Kohii::Dev -- $prev, ${prev?.container}"
    }
    playback.lifecycleState = lifecycleOwner.lifecycle.currentState
    playback.onAdded()
  }

  internal fun removePlayback(playback: Playback<*>) {
    if (playback.isAttached) {
      if (playback.isActive) onPlaybackInActive(playback)
      onPlaybackDetached(playback)
    }
    if (playbacks.remove(playback.container) === playback) playback.onRemoved()
  }

  internal fun onRemoveContainer(container: Any) {
    playbacks[container]?.let { removePlayback(it) }
  }

  private fun onPlaybackAttached(playback: Playback<*>) {
    playback.onAttached()
  }

  private fun onPlaybackDetached(playback: Playback<*>) {
    playback.onDetached()
  }

  private fun onPlaybackActive(playback: Playback<*>) {
    playback.onActive()
  }

  private fun onPlaybackInActive(playback: Playback<*>) {
    playback.onInActive()
  }

  internal fun <RENDERER : Any> acquireRenderer(
    playback: Playback<*>,
    playable: Playable<RENDERER>
  ): RENDERER? {
    val renderer = group.findRendererProvider(playable)
        .acquireRenderer(playback, playable.media)
    playback.onAttachRenderer(renderer)
    return renderer
  }

  internal fun <RENDERER : Any> releaseRenderer(
    playback: Playback<*>,
    playable: Playable<RENDERER>
  ) {
    val renderer = playable.bridge.playerView
    playback.onDetachRenderer(renderer)
    group.findRendererProvider(playable)
        .releaseRenderer(playback, playable.media, playable.bridge.playerView)
  }

  // Public APIs

  @Suppress("MemberVisibilityCanBePrivate")
  fun <RENDERER : Any> registerRendererProvider(
    type: Class<RENDERER>,
    provider: RendererProvider<RENDERER>
  ) {
    group.registerRendererProvider(type, provider)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun <RENDERER : Any> unregisterRendererProvider(provider: RendererProvider<RENDERER>) {
    group.unregisterRendererProvider(provider)
  }

  fun attach(vararg views: View): Manager {
    views.forEach { this.attachHost(it) }
    return this
  }

  fun attach(vararg hosts: Host<*>): Manager {
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
    views.forEach { this.detachHost(it) }
    return this
  }
}
