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
import android.os.Handler
import android.os.Message
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.app.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Manager.OnSelectionListener
import kohii.distanceTo
import kohii.media.VolumeInfo
import kohii.partitionToMutableSets
import kohii.v1.Prioritized
import java.util.ArrayDeque
import kotlin.properties.Delegates

class Group(
  internal val master: Master,
  internal val activity: ComponentActivity
) : LifecycleObserver, Handler.Callback {

  companion object {
    const val DELAY = 2 * 1000L / 60 /* about 2 frames */
    const val MSG_REFRESH = 1

    private val managerComparator = Comparator<Manager> { o1, o2 -> o2.compareTo(o1) }
  }

  override fun handleMessage(msg: Message): Boolean {
    if (msg.what == MSG_REFRESH) this.refresh()
    return true
  }

  internal val managers = ArrayDeque<Manager>()
  internal val organizer = Organizer()

  private var stickyManager by Delegates.observable<Manager?>(
      initialValue = null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        if (to != null) { // a Manager is promoted
          to.sticky = true
          managers.push(to)
        } else {
          require(from != null && from.sticky)
          if (managers.peek() === from) {
            from.sticky = false
            managers.pop()
          }
        }
      }
  )

  internal var volumeInfoUpdater: VolumeInfo by Delegates.observable(
      initialValue = VolumeInfo(),
      onChange = { _, from, to ->
        if (from == to) return@observable
        // Update VolumeInfo of all Hosts. This operation will then callback to this #applyVolumeInfo
        managers.forEach { it.volumeInfoUpdater = to }
      }
  )

  internal val volumeInfo: VolumeInfo
    get() = volumeInfoUpdater

  private val handler = Handler(this)
  private val dispatcher = PlayableDispatcher(master)
  private val rendererProviders = mutableMapOf<Class<*>, RendererProvider<*>>()
  private val defaultRendererProvider = PlayerViewProvider()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass !== other?.javaClass) return false
    other as Group
    if (activity !== other.activity) return false
    return true
  }

  override fun hashCode(): Int {
    return activity.hashCode()
  }

  internal val playbacks: Collection<Playback>
    get() = managers.flatMap { it.playbacks.values }

  @OnLifecycleEvent(ON_CREATE)
  internal fun onCreate() {
    master.onGroupCreated(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    handler.removeCallbacksAndMessages(null)
    owner.lifecycle.removeObserver(this)
    master.onGroupDestroyed(this)
  }

  @OnLifecycleEvent(ON_START)
  internal fun onStart() {
    dispatcher.onStart()
  }

  @OnLifecycleEvent(ON_STOP)
  internal fun onStop() {
    dispatcher.onStop()
    handler.removeMessages(MSG_REFRESH)
  }

  internal fun findHostForContainer(container: ViewGroup): Host? {
    require(ViewCompat.isAttachedToWindow(container))
    return managers.asSequence()
        .mapNotNull { it.findHostForContainer(container) }
        .firstOrNull()
  }

  internal fun <RENDERER : Any> findRendererProvider(playable: Playable<RENDERER>): RendererProvider<RENDERER> {
    val cache = rendererProviders[playable.rendererType]
        ?: rendererProviders.asSequence().firstOrNull {
          // If there is a RendererProvider of subclass, we can use it.
          playable.rendererType.isAssignableFrom(it.key)
        }?.value
    @Suppress("UNCHECKED_CAST")
    return requireNotNull(cache) as RendererProvider<RENDERER>
  }

  internal fun <RENDERER : Any> registerRendererProvider(
    type: Class<RENDERER>,
    provider: RendererProvider<RENDERER>
  ) {
    rendererProviders.put(type, provider)
        ?.clear()
  }

  internal fun <RENDERER : Any> unregisterRendererProvider(provider: RendererProvider<RENDERER>) {
    rendererProviders
        .filterValues { it === provider }
        .keys
        .forEach {
          rendererProviders.remove(it)
              ?.clear()
        }
  }

  internal fun onRefresh() {
    handler.removeMessages(MSG_REFRESH)
    handler.sendEmptyMessageDelayed(MSG_REFRESH, DELAY)
  }

  private fun refresh() {
    val playbacks = this.playbacks // save a cache to prevent re-mapping
    playbacks.forEach { it.onRefresh() }

    val toPlay = linkedSetOf<Playback>() // Need the order.
    val toPause = ArraySet<Playback>()

    managers.forEach {
      val (canPlay, canPause) = it.splitPlaybacks()
      toPlay.addAll(canPlay)
      toPause.addAll(canPause)
    }

    val oldSelection = organizer.selection
    val newSelection = organizer.selectFinal(toPlay)

    // biggest Rect cover all selected Playbacks
    val cover = newSelection.fold(Rect()) { acc, playback ->
      acc.union(playback.token.containerRect)
      return@fold acc
    }

    // Update distanceToPlay
    val target = (cover.centerX() to cover.width() / 2) to (cover.centerY() to cover.height() / 2)
    if (target.first.second > 0 && target.second.second > 0) {
      playbacks.partitionToMutableSets(
          predicate = { it.isActive },
          transform = { it }
      )
          .also { (active, inactive) ->
            inactive.forEach { it.distanceToPlay = Int.MAX_VALUE }
            // TODO better way that doesn't require allocating new collection?
            (active - newSelection).sortedBy { it.token.containerRect distanceTo target }
                .forEachIndexed { index, playback -> playback.distanceToPlay = index }
          }
      newSelection.forEach { it.distanceToPlay = 0 }
    }

    val playables = master.playables.keys
    (toPause + toPlay + oldSelection - newSelection)
        .mapNotNull { playback -> playables.find { it.playback === playback } }
        .forEach { dispatcher.pause(it) }

    newSelection
        .mapNotNull { playback -> playables.find { it.playback === playback } }
        .forEach { dispatcher.play(it) }

    val grouped = newSelection.groupBy { it.manager }
    this.managers.asSequence()
        .filter { it.host is OnSelectionListener }
        .forEach {
          (it.host as OnSelectionListener).onSelection(grouped.getOrElse(it) { emptyList() })
        }
  }

  internal fun onManagerDestroyed(manager: Manager) {
    if (stickyManager === manager) stickyManager = null
    if (managers.remove(manager)) master.onGroupUpdated(this)
    if (managers.size == 0) {
      this.unregisterRendererProvider(defaultRendererProvider)
      rendererProviders.onEach { it.value.clear() }
          .clear()
    }
  }

  // This operation should:
  // - Ensure the order of Manager by its Priority
  // - Ensure stickyManager is in the head.
  internal fun onManagerCreated(manager: Manager) {
    if (managers.size == 0) {
      this.registerRendererProvider(PlayerView::class.java, defaultRendererProvider)
    }

    val updated: Boolean
    // 1. Pop out the sticky Manager if available.
    val sticky = if (managers.peek()?.sticky == true) managers.pop() else null

    // 2. Add the Manager to the queue using its Priority if available.
    if (manager.host is Prioritized) {
      if (!managers.contains(manager)) {
        updated = managers.add(manager)
        val temp = managers.sortedWith(managerComparator)
        managers.clear()
        managers.addAll(temp)
      } else
        updated = false
    } else {
      updated = managers.add(manager)
    }

    // 3. Push the sticky Manager back to head of the queue.
    if (sticky != null) managers.push(sticky)

    if (updated) master.onGroupUpdated(this)
  }

  // Expected result:
  // - If 'manager' is not added to Group yet, through Exception.
  // - The 'manager' should be on the head of the 'managers' queue after this operation, though the
  // 'managers' queue should still be able to remember its original location for the unsticking.
  internal fun stick(manager: Manager) {
    stickyManager = manager
  }

  internal fun unstick(manager: Manager?) {
    if (manager == null || stickyManager === manager) stickyManager = null
  }
}
