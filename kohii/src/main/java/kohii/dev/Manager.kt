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
import com.google.android.exoplayer2.ui.PlayerView
import kohii.partitionToArrayLists

class Manager(
  val group: Group,
  val host: Any,
  val lifecycleOwner: LifecycleOwner
) : LifecycleObserver {

  private val hosts = linkedSetOf<Host>()
  // All Playbacks, including attached/detached ones.
  internal val playbacks = mutableMapOf<Any, Playback<*>>()
  // Attached Playbacks only.
  // True -> Attached, Active, False -> Attached, InActive.
  private val playbackStates = mutableMapOf<Playback<*>, Boolean>()
  private val playerViewProvider = PlayerViewProvider()

  @OnLifecycleEvent(ON_ANY)
  internal fun onAnyEvent(owner: LifecycleOwner) {
    playbacks.forEach { it.value.lifecycleState = owner.lifecycle.currentState }
  }

  @OnLifecycleEvent(ON_CREATE)
  internal fun onCreate() {
    group.managers.add(this)
    this.registerRendererProvider(PlayerView::class.java, playerViewProvider)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    ArrayList(playbacks.values)
        .onEach { removePlayback(it) /* also modify playbacks */ }
        .clear()
    this.unregisterRendererProvider(playerViewProvider)
    owner.lifecycle.removeObserver(this)
    group.managers.remove(this)
  }

  @OnLifecycleEvent(ON_START)
  internal fun onStart() {
    refresh()
  }

  @OnLifecycleEvent(ON_STOP)
  internal fun onStop() {
    playbackStates.filterValues { it }
        .forEach { onPlaybackInActive(it.key) }
    refresh()
  }

  internal fun findHostForContainer(container: ViewGroup): Host? {
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
      val state = playbackStates[playback]
      if (state != null) {
        if (state) onPlaybackInActive(playback)
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

  internal fun registerHost(view: View) {
    view.doOnAttach { v ->
      val host = Host[this@Manager, v]
      if (hosts.add(host)) host.onAdded()
    }
    view.doOnDetach { v ->
      hosts.filter { it.hostRoot === v }
          .forEach {
            if (hosts.remove(it)) it.onRemoved()
          }
    }
  }

  internal fun refresh() {
    group.onRefresh()
  }

  private fun refreshPlaybackStates(): Pair<MutableCollection<Playback<*>> /* Active */, MutableCollection<Playback<*>> /* InActive */> {
    val toActive = playbackStates.filter { !it.value && it.key.token.shouldPrepare() }
        .keys
    val toInActive = playbackStates.filter { it.value && !it.key.token.shouldPrepare() }
        .keys

    toActive.forEach { onPlaybackActive(it) }
    toInActive.forEach { onPlaybackInActive(it) }

    return playbackStates.asSequence()
        .partitionToArrayLists(
            predicate = { it.value },
            transform = { it.key }
        )
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
    val state = playbackStates[playback]
    if (state != null) {
      if (state) onPlaybackInActive(playback)
      onPlaybackDetached(playback)
    }
    if (playbacks.remove(playback.container) === playback) playback.onRemoved()
  }

  private fun onPlaybackAttached(playback: Playback<*>) {
    playbackStates[playback] = false
    playback.onAttached()
  }

  private fun onPlaybackDetached(playback: Playback<*>) {
    playbackStates.remove(playback)
    playback.onDetached()
  }

  private fun onPlaybackActive(playback: Playback<*>) {
    playbackStates[playback] = true
    playback.onActive()
  }

  private fun onPlaybackInActive(playback: Playback<*>) {
    playbackStates[playback] = false
    playback.onInActive()
  }

  internal fun splitPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    val toPlay = HashSet<Playback<*>>()

    val hostToPlaybacks = activePlaybacks.filter { it.host.allowToPlay(it) }
        .groupBy { it.host }

    hosts.asSequence()
        .filter { !hostToPlaybacks[it].isNullOrEmpty() }
        .map { it.selectToPlay(hostToPlaybacks.getValue(it)) }
        .firstOrNull { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          activePlaybacks.removeAll(it)
        }

    activePlaybacks.addAll(inactivePlaybacks)
    return toPlay to activePlaybacks
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
}
