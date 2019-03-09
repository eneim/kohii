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
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.PlaybackInfo

/**
 * Bind to an Activity, to manage [PlaybackManager]s inside.
 *
 * This is to support many [PlaybackManager]s in one Activity. For example: ViewPager whose pages
 * are Fragments, List/Detail UI, etc. When a [PlaybackManager] dispatch the refresh event, it will
 * call this class's [dispatchManagerRefresh] so that all other [PlaybackManager] in the same host
 * will also be notified.
 */
class ActivityContainer(
  internal val kohii: Kohii,
  internal val activity: Activity,
  internal val selector: (Collection<Playback<*>>) -> Collection<Playback<*>> = defaultSelector
) : LifecycleObserver, Playback.Callback {

  private val prioritizedManagers = HashSet<PlaybackManager>()
  private val standardManagers = HashSet<PlaybackManager>()
  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()

  private val dispatcher by lazy { ManagerDispatcher(this) }

  companion object {
    val managerComparator = Comparator<PlaybackManager> { o1, o2 -> o2.compareTo(o1) }
    val defaultSelector: (Collection<Playback<*>>) -> Collection<Playback<*>> =
      { listOfNotNull(it.firstOrNull()) }
  }

  internal fun attachPlaybackManager(playbackManager: PlaybackManager) {
    if (kohii.managers.size == 1) {
      kohii.onFirstManagerOnline()
    }
    if (playbackManager.provider is Prioritized) {
      prioritizedManagers.add(playbackManager)
    } else {
      standardManagers.add(playbackManager)
    }
  }

  // Called by PlaybackManager
  internal fun detachPlaybackManager(playbackManager: PlaybackManager) {
    if (playbackManager.provider is Prioritized) {
      prioritizedManagers.remove(playbackManager)
    } else standardManagers.remove(playbackManager)

    if (kohii.managers.isEmpty()) {
      kohii.onLastManagerOffline()
    }
  }

  internal fun findSuitableManger(target: Any): PlaybackManager? {
    return (this.prioritizedManagers + this.standardManagers) // Order is important.
        .firstOrNull { it.findSuitableContainer(target) != null }
  }

  internal fun dispatchManagerRefresh() {
    // Will dispatch with a small delay, to prevent aggressive pushing.
    dispatcher.dispatchRefresh()
  }

  internal fun trySavePlaybackInfo(playback: Playback<*>) {
    if (playback.playable.tag != Playable.NO_TAG) {
      mapPlayableTagToInfo[playback.playable.tag] = playback.playable.playbackInfo
    }
  }

  internal fun tryRestorePlaybackInfo(playback: Playback<*>) {
    if (playback.playable.tag != Playable.NO_TAG) {
      val info = mapPlayableTagToInfo.remove(playback.playable.tag)
      if (info != null) playback.playable.playbackInfo = info
    }
  }

  @Suppress("UNUSED_PARAMETER")
  @OnLifecycleEvent(ON_CREATE)
  fun onOwnerCreate(owner: LifecycleOwner) {
    playbackDispatcher.onAttached()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    playbackDispatcher.onDetached()
    // Eagerly detach all PlaybackManager if there is any.
    ((standardManagers + prioritizedManagers) as MutableSet) // Kotlin sdk should not change this.
        .onEach { detachPlaybackManager(it) }
        .clear()

    owner.lifecycle.removeObserver(this)
    kohii.owners.remove(owner)
  }

  override fun onRemoved(playback: Playback<*>) {
    playbackDispatcher.onPlaybackRemoved(playback)
  }

  private val playbackDispatcher = PlaybackDispatcher()

  internal fun refreshPlaybacks() {
    // Steps
    // 1. Collect candidates from children PlaybackManagers
    // 2. Pick the one to play.
    // 3. Pause other Playbacks.
    // 4. Play the chosen one.

    // 1. Collect candidates from children PlaybackManagers
    // candidates.clear()
    val toPlay = LinkedHashSet<Playback<*>>()
    val toPause = HashSet<Playback<*>>()

    var picked = false
    var prioritized = false

    if (prioritizedManagers.isNotEmpty()) {
      prioritized = true
      prioritizedManagers.sortedWith(managerComparator)
          .forEach {
            val candidates = it.partitionPlaybacks()
            toPause.addAll(candidates.second)
            if (!picked) {
              if (candidates.first.isNotEmpty()) {
                toPlay.addAll(candidates.first)
                picked = true
              }
            } else {
              toPause.addAll(candidates.first)
            }
          }
    }

    if (!prioritized) { // There is no 'prioritized' Manager.
      standardManagers.map { it.partitionPlaybacks() }
          .forEach {
            toPlay.addAll(it.first)
            toPause.addAll(it.second)
          }
    } else { // Other Manager is prioritized, here we just collect Playback to pause.
      standardManagers.map { it.partitionPlaybacks() }
          .flatMap { it.first + it.second }
          .also { toPause.addAll(it) }
    }

    val selected = selector.invoke(toPlay)
    (toPause + toPlay - selected).forEach { playbackDispatcher.pause(it) }
    selected.forEach { playbackDispatcher.play(it) }
  }

  override fun toString(): String {
    return "Root::${Integer.toHexString(hashCode())}"
  }
}