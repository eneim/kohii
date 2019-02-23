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
class RootManager(
  val kohii: Kohii
) : LifecycleObserver, Playback.Callback {

  private val mapOwnerToManager = HashMap<LifecycleOwner, PlaybackManager>()
  private val prioritizedManager = HashSet<PlaybackManager>()
  private val originalManager = HashSet<PlaybackManager>()

  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()
  private val dispatcher by lazy { RootDispatcher(this) }

  companion object {
    val managerComparator = Comparator<PlaybackManager> { o1, o2 -> o2.compareTo(o1) }
  }

  internal fun attachPlaybackManager(
    lifecycleOwner: LifecycleOwner,
    playbackManager: PlaybackManager
  ) {
    if (kohii.playbackManagerCache.size == 1) {
      kohii.onFirstManagerOnline()
    }
    if (!this.mapOwnerToManager.containsKey(lifecycleOwner)) {
      this.mapOwnerToManager[lifecycleOwner] = playbackManager
    }
    if (playbackManager.containerProvider is Prioritized) {
      prioritizedManager.add(playbackManager)
    } else {
      originalManager.add(playbackManager)
    }
  }

  internal fun detachPlaybackManager(playbackManager: PlaybackManager) {
    val cache = this.mapOwnerToManager.filterValues { it === playbackManager }
    for (item in cache) this.mapOwnerToManager.remove(item.key)
    if (playbackManager.containerProvider is Prioritized) prioritizedManager.remove(playbackManager)
    else originalManager.remove(playbackManager)

    if (kohii.playbackManagerCache.isEmpty()) {
      kohii.onLastManagerOffline()
    }
  }

  internal fun dispatchManagerRefresh(): Boolean {
    // this.mapOwnerToManager.forEach { it.value.dispatchRefreshAllInternal() }
    dispatcher.dispatchRefresh()
    return true
  }

  internal fun trySavePlaybackInfo(playback: Playback<*>) {
    if (playback.tag != Playable.NO_TAG) {
      mapPlayableTagToInfo[playback.tag] = playback.playable.playbackInfo
    }
  }

  internal fun tryRestorePlaybackInfo(playback: Playback<*>) {
    if (playback.tag != Playable.NO_TAG) {
      val info = mapPlayableTagToInfo.remove(playback.tag)
      if (info != null) playback.playable.playbackInfo = info
    }
  }

  @Suppress("UNUSED_PARAMETER")
  @OnLifecycleEvent(ON_CREATE)
  fun onOwnerCreate(lifecycleOwner: LifecycleOwner) {
    playbackDispatcher.onAttached()
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(lifecycleOwner: LifecycleOwner) {
    playbackDispatcher.onDetached()
    lifecycleOwner.lifecycle.removeObserver(this)
    kohii.parents.remove(lifecycleOwner)
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
    val toPlay = HashSet<Playback<*>>()
    val toPause = HashSet<Playback<*>>()

    var picked = false
    var prioritized = false

    if (prioritizedManager.isNotEmpty()) {
      prioritized = true
      prioritizedManager.sortedWith(managerComparator)
          .forEach { manager ->
            val candidates = manager.fetchPlaybackCandidates()
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

    // There is no 'prioritized' Manager.
    if (!prioritized) {
      originalManager.map { it.fetchPlaybackCandidates() }
          .forEach {
            toPlay.addAll(it.first)
            toPause.addAll(it.second)
          }
    } else {
      originalManager.map { it.fetchPlaybackCandidates() }
          .flatMap { it.first + it.second }
          .also {
            toPause.addAll(it)
          }
    }

    val selected = toPlay.firstOrNull() // TODO better selection.

    (if (selected == null) (toPause + toPlay) else ((toPause + toPlay - selected))).forEach {
      playbackDispatcher.pause(it)
    }
    if (selected != null) playbackDispatcher.play(selected)
  }

  override fun toString(): String {
    return "Root::${Integer.toHexString(hashCode())}"
  }
}