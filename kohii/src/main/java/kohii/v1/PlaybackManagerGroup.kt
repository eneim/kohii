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

package kohii.v1

import android.app.Activity
import androidx.collection.ArraySet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.VolumeInfo
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Manage an [FragmentActivity], to control [PlaybackManager]s inside.
 *
 * This is to support many [PlaybackManager]s in one Activity. For example: ViewPager whose pages
 * are Fragments, List/Detail UI, etc. When a [PlaybackManager] dispatch the refresh event, it will
 * call this class's [onManagerRefresh] so that all other [PlaybackManager] in the same host
 * will also be notified.
 *
 * In the implementation, I use LinkedHashMap/LinkedHashSet where the order is important, and
 * ArrayMap/ArraySet/ArrayList otherwise.
 * Where the number of entries are expected to grow, normal HashMap/HashSet will be used.
 */
class PlaybackManagerGroup(
  internal val kohii: Kohii,
  internal val activity: FragmentActivity,
  internal val organizer: PlaybackOrganizer = SinglePlayerOrganizer()
) : LifecycleObserver, Playback.Callback {

  private var promotedManager: PlaybackManager? = null
  private val stickyManagers by lazy(NONE) { LinkedHashSet<PlaybackManager>() }
  private val commonManagers = ArraySet<PlaybackManager>()

  private val managerDispatcher = PlaybackManagerDispatcher(this)
  private val playbackDispatcher = PlaybackDispatcher(kohii)

  internal val lock = AtomicBoolean(false)
  internal val volumeInfo = VolumeInfo()

  internal val rendererPools = HashMap<Class<*>, RendererPool<*>>()

  companion object {
    internal val managerComparator = Comparator<PlaybackManager> { o1, o2 -> o2.compareTo(o1) }
  }

  internal fun attachPlaybackManager(playbackManager: PlaybackManager): Boolean {
    if (kohii.managers.size == 1) {
      kohii.onFirstManagerOnline()
    }
    val updated: Boolean
    if (playbackManager.provider is Prioritized) {
      // We do not expect too many of this operation.
      // The technique below is to ensure all prioritized Manager are added in order made by the
      // Comparator they follow.
      // We do not use TreeSet or any implementation of SortedSet, because if 2 items are equal due
      // to Comparator result, they are considered identical in SortedSet, which will prevent us from
      // adding Managers of same priorities.
      updated = stickyManagers.add(playbackManager)
      val temp = stickyManagers.sortedWith(managerComparator)
      stickyManagers.clear()
      stickyManagers.addAll(temp)
    } else {
      updated = commonManagers.add(playbackManager)
    }
    if (updated) {
      playbackManager.volumeInfo.setTo(this.volumeInfo)
      playbackManager.targetHosts.forEach { it.volumeInfo.setTo(this.volumeInfo) }
    }
    return updated
  }

  // Called by PlaybackManager
  internal fun detachPlaybackManager(playbackManager: PlaybackManager) {
    val handled = stickyManagers.remove(playbackManager) or commonManagers.remove(playbackManager)
    if (promotedManager === playbackManager) promotedManager = null

    kohii.managers.remove(playbackManager.owner)
    if (handled) {
      if (kohii.managers.isEmpty) kohii.onLastManagerOffline()
    }
  }

  internal fun onManagerRefresh() {
    // Will dispatch with a small delay, to prevent aggressive pushing.
    managerDispatcher.dispatchRefresh()
  }

  @OnLifecycleEvent(ON_START)
  fun onOwnerStart() {
    playbackDispatcher.onStart()
  }

  @OnLifecycleEvent(ON_STOP)
  fun onOwnerStop() {
    playbackDispatcher.onStop()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    // playbackDispatcher.onStop()
    organizer.deselect(organizer.selection)
    val managers = this.managers()
    if (!activity.isChangingConfigurations) {
      managers.flatMap { m -> kohii.playables.filter { it.manager === m } }
          .forEach { kohii.manualPlayableRecord.remove(it) }
    }
    // Eagerly detach all PlaybackManager if there is any.
    // Each operation will also modify the related Set.
    managers.onEach { it.onOwnerDestroy(it.owner) }
        .clear()
    stickyManagers.clear()
    commonManagers.clear()
    promotedManager = null

    this.rendererPools.onEach { it.value.cleanUp() }
        .clear()

    owner.lifecycle.removeObserver(this)
    managerDispatcher.onDestroyed()
    if (owner is Activity) kohii.groups.remove(owner)
  }

  // Playback.Callback#onRemoved(Playback)
  override fun onRemoved(playback: Playback<*>) {
    playbackDispatcher.onPlaybackRemoved(playback)
  }

  internal fun refreshPlaybacks() {
    // Steps
    // 1. Collect candidates from children PlaybackManagers
    // 2. Pick the one to play.
    // 3. Pause other Playbacks.
    // 4. Play the chosen one(s).

    // 1. Collect candidates from children PlaybackManagers
    val toPlay = LinkedHashSet<Playback<*>>() // need the ordering.
    val toPause = ArraySet<Playback<*>>()

    var picked = false // if true --> prioritized Managers has candidates picked for playing.
    var prioritized = false

    // Promoted Manager always win.
    val promotedManager = this.promotedManager
    if (promotedManager != null) {
      val (canPlay, canPause) = promotedManager.partitionPlaybacks()
      toPause.addAll(canPause)
      if (!picked) {
        if (canPlay.isNotEmpty()) {
          toPlay.addAll(canPlay)
          picked = true
        }
      } else {
        toPause.addAll(canPlay)
      }
      prioritized = picked
    }

    if (!prioritized) {
      if (stickyManagers.isNotEmpty()) {
        prioritized = true
        stickyManagers.forEach {
          val (canPlay, canPause) = it.partitionPlaybacks()
          toPause.addAll(canPause)
          if (!picked) {
            if (canPlay.isNotEmpty()) {
              toPlay.addAll(canPlay)
              picked = true
            }
          } else {
            toPause.addAll(canPlay)
          }
        }
      }
    }

    if (!prioritized) { // There is no 'prioritized' Manager.
      commonManagers.map { it.partitionPlaybacks() }
          .forEach { (canPlay, canPause) ->
            toPlay.addAll(canPlay)
            toPause.addAll(canPause)
          }
    } else { // Other managers are prioritized, here we just collect Playback to pause.
      commonManagers.flatMap {
        it.refreshPlaybackStates()
            .let { pair -> pair.first + pair.second }
      }
          .also { toPause.addAll(it) }
    }

    if (lock.get()) {
      organizer.deselect(organizer.selection)
      (toPause + toPlay).forEach {
        playbackDispatcher.pause(it)
      }
    } else {
      val oldSelection = organizer.selection
      val newSelection = organizer.select(toPlay)
      (toPause + toPlay - newSelection - oldSelection).forEach {
        playbackDispatcher.pause(it)
      }

      if (newSelection.isNotEmpty()) {
        oldSelection.forEach { playbackDispatcher.pause(it) }
        newSelection.onEach { playbackDispatcher.play(it) }
            .groupBy { it.manager }
            .forEach { (m, p) -> m.selectionCallbacks.value.onSelection(p) }
      } else {
        // TODO consider headless playback params here?
        val playback = oldSelection.firstOrNull()
//        if (playback != null) {
//          val playable = playback.playable
//          val params = playable.config.headlessPlaybackParams
//          if (playable.isPlaying() && params != null && params.enabled) {
//            kohii.enterHeadlessPlayback(playback, params)
//          }
//        }
      }
    }
  }

  internal fun managers(): MutableSet<PlaybackManager> =
    mutableSetOf<PlaybackManager>().apply {
      promotedManager?.also { add(it) }
      addAll(stickyManagers)
      addAll(commonManagers)
    }

  override fun toString(): String {
    return "Root::${Integer.toHexString(hashCode())}::$activity"
  }

  internal fun promote(manager: PlaybackManager?) {
    if (manager != null) {
      this.stickyManagers.remove(manager)
      this.commonManagers.remove(manager)
    } else {
      if (this.promotedManager != null) {
        if (promotedManager!!.provider is Prioritized) {
          stickyManagers.add(promotedManager!!)
          val temp = stickyManagers.sortedWith(managerComparator)
          stickyManagers.clear()
          stickyManagers.addAll(temp)
        } else {
          commonManagers.add(promotedManager!!)
        }
      }
    }
    this.promotedManager = manager
  }
}
