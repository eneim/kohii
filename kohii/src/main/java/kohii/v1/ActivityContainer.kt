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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.PlaybackInfo
import kohii.v1.exo.PlayerViewCreator

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
  internal val activity: FragmentActivity,
    // TODO make this configurable
  private val selector: (Collection<Playback<*>>) -> Collection<Playback<*>> = defaultSelector
) : LifecycleObserver, Playback.Callback {

  private val prioritizedManagers = HashSet<PlaybackManager>()
  private val standardManagers = HashSet<PlaybackManager>()
  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()

  private val dispatcher by lazy { ActivityContainerDispatcher(this) }
  private val defaultOutputHolderPool by lazy { OutputHolderPool(2, PlayerViewCreator()) }

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
    playbackManager.registerOutputHolderPool(this.defaultOutputHolderPool)
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
    return this.managers()
        .firstOrNull { it.findHostForTarget(target) != null }
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

  @OnLifecycleEvent(ON_CREATE)
  fun onOwnerCreate() {
    playbackDispatcher.onAttached()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(owner: LifecycleOwner) {
    playbackDispatcher.onDetached()
    if (!activity.isChangingConfigurations) {
      kohii.manualFlag.clear()
    }
    // Eagerly detach all PlaybackManager if there is any.
    this.managers()
        .onEach {
          detachPlaybackManager(it)
        }
        .clear()

    owner.lifecycle.removeObserver(this)
    dispatcher.onContainerDestroyed()
    kohii.owners.remove(owner)
  }

  override fun onRemoved(playback: Playback<*>) {
    playbackDispatcher.onPlaybackRemoved(playback)
  }

  private val playbackDispatcher = PlaybackDispatcher(kohii)

  internal fun refreshPlaybacks() {
    // Steps
    // 1. Collect candidates from children PlaybackManagers
    // 2. Pick the one to play.
    // 3. Pause other Playbacks.
    // 4. Play the chosen one.

    // 1. Collect candidates from children PlaybackManagers
    val toPlay = LinkedHashSet<Playback<*>>() // need the ordering.
    val toPause = HashSet<Playback<*>>()

    var picked = false // true --> prioritized Manager has candidates picked for playing.
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
    } else { // Other managers are prioritized, here we just collect Playback to pause.
      standardManagers.flatMap { it.refreshPlaybacks() }
          .also { toPause.addAll(it) }
    }

    val selected = selector.invoke(toPlay)
    (toPause + toPlay - selected).forEach { playbackDispatcher.pause(it) }
    selected.forEach { playbackDispatcher.play(it) }
  }

  // Hope that Kotlin would not change the MutableSet.
  internal fun managers(): MutableSet<PlaybackManager> =
    (this.prioritizedManagers + this.standardManagers) as MutableSet<PlaybackManager> // Order is important.

  override fun toString(): String {
    return "Root::${Integer.toHexString(hashCode())}"
  }
}
