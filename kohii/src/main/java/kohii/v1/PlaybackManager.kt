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

import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.isChangingConfig
import kohii.media.PlaybackInfo
import kohii.v1.Playback.Token
import java.util.TreeMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Definition of an object that can manage multiple [Playback]s.
 */
abstract class PlaybackManager(
  protected val kohii: Kohii,
  protected val parent: RootManager,
  protected val containerProvider: ContainerProvider
    // private val playbackSelector: PlaybackSelector = DEFAULT_SELECTOR
) : LifecycleObserver {

  companion object {
    val TOKEN_COMPARATOR: Comparator<Token> = Comparator { o1, o2 -> o1.compareTo(o2) }
    /* val DEFAULT_SELECTOR = object : PlaybackSelector {
      override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
        return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
      }
    } */
  }

  interface PlaybackSelector {
    fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>>
  }

  private val containers by lazy {
    LinkedHashSet<Container>()
  }

  private val attachFlag = AtomicBoolean(false)
  private val playbackDispatcher = PlaybackDispatcher()
  private var dispatcher: Dispatcher? = null

  private val mapAttachedPlaybackToTime = LinkedHashMap<Playback<*>, Long>()
  // Weak map, so detached Playback can be cleared if not referred anymore.
  // TODO need a mechanism to release those weak Playbacks.
  private val mapDetachedPlaybackToTime = WeakHashMap<Playback<*>, Long>()

  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any? /* Target */, Playback<*>>()
  // Manager has responsibility to memorise its Playback's info.
  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()

  // Candidates to hold playbacks for a refresh call.
  private val candidates = TreeMap<Token, Playback<*>>(TOKEN_COMPARATOR)

  /// [BEGIN] Internal API

  @CallSuper
  @OnLifecycleEvent(ON_CREATE)
  protected open fun onOwnerCreate(owner: LifecycleOwner) {
    if (kohii.playbackManagerCache.size == 1) {
      kohii.onFirstManagerOnline()
    }
    this.onAttached()
  }

  @CallSuper
  @OnLifecycleEvent(ON_START)
  protected open fun onOwnerStart(owner: LifecycleOwner) {
    mapAttachedPlaybackToTime.mapNotNull { it.key }
        .forEach {
          Log.w("Kohii::M", "start, cache: ${mapTargetToPlayback.containsValue(it)}")
          if (kohii.mapPlayableToManager[it.playable] == null) {
            kohii.mapPlayableToManager[it.playable] = this
            this.tryRestorePlaybackInfo(it)
            it.onActive()
            // TODO [20180905] double check the usage of 'shouldPrepare()' here.
            if (it.token?.shouldPrepare() == true) it.prepare()
          }
        }

    this.dispatchRefreshAll()
  }

  @CallSuper
  @OnLifecycleEvent(ON_STOP)
  protected open fun onOwnerStop(owner: LifecycleOwner) {
    val configChange = containerProvider.provideContext()
        .isChangingConfig()
    mapAttachedPlaybackToTime.mapNotNull { it.key }
        .forEach {
          it.onInActive()
          val playable = it.playable
          // Only pause this playback if
          // - [1] config change is not happening and
          // - [2] the playable is managed by this manager, or by no-one.
          // FYI: The Playable instances holds the actual playback resource. It is not managed by
          // anything else when the Activity is destroyed and to be recreated (config change).
          if (!configChange) {
            if (kohii.mapPlayableToManager[playable] === this) {
              it.pause()
              this.trySavePlaybackInfo(it)
              // TODO [20190206] also release?
              // There is no recreation. If this manager is managing the playable, unload the Playable.
              kohii.mapPlayableToManager[playable] = null
            }
          }
        }
  }

  @CallSuper
  @OnLifecycleEvent(ON_DESTROY)
  internal open fun onOwnerDestroy(owner: LifecycleOwner) {
    // Wrap by an ArrayList because we also remove entry while iterating by performRemovePlayback
    (ArrayList(mapTargetToPlayback.values).apply {
      this.forEach {
        performRemovePlayback(it)
        if (kohii.mapPlayableToManager[it.playable] === this@PlaybackManager) {
          kohii.mapPlayableToManager[it.playable] = null
        }
      }
    }).clear()

    (ArrayList(this.containers).also { items ->
      items.forEach { this.unregisterContainer(it) }
    }).clear()

    owner.lifecycle.removeObserver(this)
    kohii.playbackManagerCache.remove(owner)
        ?.also {
          it.onDetached()
        }
    this.onDetached() // in case of failure in the lines above.
    this.parent.detachPlaybackManager(this)

    val configChange = containerProvider.provideContext()
        .isChangingConfig()
    // If this is the last Manager, and it is not a config change, clean everything.
    if (kohii.playbackManagerCache.isEmpty()) {
      kohii.onLastManagerOffline()
      if (!configChange) kohii.cleanUp()
    }
  }

  private fun trySavePlaybackInfo(playback: Playback<*>) {
    if (playback.tag != Playable.NO_TAG) {
      mapPlayableTagToInfo[playback.tag] = playback.playable.playbackInfo
    }
  }

  private fun tryRestorePlaybackInfo(playback: Playback<*>) {
    if (playback.tag != Playable.NO_TAG) {
      val info = mapPlayableTagToInfo.remove(playback.tag)
      if (info != null) playback.playable.playbackInfo = info
    }
  }

  protected open fun onAttached() {
    if (dispatcher == null) dispatcher = Dispatcher(this)
    if (attachFlag.compareAndSet(false, true)) {
      playbackDispatcher.onAttached()
      containers.forEach { it.onHostAttached() }
    }
  }

  protected open fun onDetached() {
    if (attachFlag.compareAndSet(true, false)) {
      containers.forEach { it.onHostDetached() }
      playbackDispatcher.onDetached()
    }

    dispatcher?.removeCallbacksAndMessages(null)
    dispatcher = null
  }

  internal fun registerContainer(container: Container) {
    this.containers.add(container)
  }

  internal fun unregisterContainer(container: Container) {
    this.containers.remove(container)
  }

  internal open fun dispatchRefreshAll() {
    if (!this.parent.dispatchManagerRefresh()) this.dispatchRefreshAllInternal()
  }

  internal open fun dispatchRefreshAllInternal() {
    dispatcher?.dispatchRefreshAll()
  }

  // Important. Do the refresh stuff. Change the playback items, etc.
  /**
   * This method is triggered once there is scroll change in the UI Hierarchy, including changing from
   * idle to scroll or vice versa.
   *
   * This method is also be called when there is update in the content, for example Target of a Playback
   * is attached/detached or the Manager itself is attached to the Window.
   *
   * Design target:
   *
   * - [1] Update current attached/detached Playbacks.
   * - [2] Pause the Playbacks those are out of interest (eg: not visible enough).
   * - [3] Allow client to select Playback that is available for a playback.
   */
  internal fun performRefreshAll() {
    candidates.clear()

    // Confirm if any detached view is re-attached to the Window. Not happen all the time.
    mapDetachedPlaybackToTime.keys.filter { it.token != null }
        .also { items ->
          dispatcher?.apply {
            // In case the container is non-RecyclerView (so all View are attached to Parent),
            // By refreshing the list, we detect Views those are out of sight, and those are not.
            items.forEach { playback -> this.dispatchTargetAvailable(playback) }
          }
        }

    // List of all attached Playbacks.
    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    // Filter out the bad ones.
    for (playback in playbacks) {
      // Get token here and use later, rather than getting it many times.
      val token = playback.token
      if (token == null) { // Should not null here, but still checking for safety.
        // In case the container is non-RecyclerView (so all View are attached to Parent),
        // By refreshing the list, we detect Views those are out of sight, and those are not.
        dispatcher?.dispatchTargetUnAvailable(playback)
      } else if (token.shouldPlay()) {
        // This will also sort the playback using Token's comparator.
        candidates[token] = playback
      }
    }

    // use Container to pick Playback for playing.
    val temp = ArrayList(candidates.values)
    val toPlay = ArrayList<Playback<*>>()
    this.containers.forEach {
      it.select(temp)
          .also { result ->
            toPlay.addAll(result)
            temp.removeAll(result)
          }
    }

    playbacks.removeAll(toPlay)
    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    // Keep only non-play-candidate ones, and pause them.
    for (playback in playbacks) {
      playbackDispatcher.pause(playback)
    }
    // Now start the play-candidate
    for (playback in toPlay) {
      playbackDispatcher.play(playback)
    }
    // Clean up cache
    candidates.clear()
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [PlaybackManager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  internal fun <T> performAddPlayback(playback: Playback<T>): Playback<T> {
    val target = playback.target
    val cache = mapTargetToPlayback[target]
    var shouldAdd = target != null // Playback must have a valid Target.
    if (shouldAdd) {
      // Not-null Target may already be the target of another Playback before.
      // Here we also make sure if we need to destroy that old Playback down first or not.
      shouldAdd = cache == null || cache !== playback
    }

    if (shouldAdd) {
      // State: there is another Playback of the same Target in the same Manager.
      // Scenario: RecyclerView recycle VH so one Target can be rebound to other Playback.
      // Action: destroy the other Playback, then add the new one.
      if (cache != null) performRemovePlayback(cache)
      mapTargetToPlayback[target] = playback
      playback.onAdded()
    }

    // In case we are adding existing one and it is already active, we refresh everything.
    if (mapAttachedPlaybackToTime.containsKey(playback)) {
      // shouldAdd is true when the target is not null and no pre-exist playback.
      if (playback.token?.shouldPlay() == true) this.dispatchRefreshAll()
    }

    // At this point, mapTargetToPlayback must contains the playback instance.
    if (BuildConfig.DEBUG) {
      if (!mapTargetToPlayback.containsValue(playback)) {
        throw IllegalStateException(
            "Could not add Playback: $playback"
        )
      }
    }

    return playback
  }

  // Permanently remove the Playback from cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapTargetToPlayback
  // - mapAttachedPlaybackToTime
  // - mapDetachedPlaybackToTime
  // - mapPlayableTagToInfo
  internal fun performRemovePlayback(playback: Playback<*>) {
    if (mapTargetToPlayback.containsValue(playback)) {
      mapTargetToPlayback.remove(playback.target)
          ?.also {
            if (it !== playback) throw IllegalStateException(
                "Illegal cache: expect $playback but get $it"
            )
            val toInActive = mapAttachedPlaybackToTime.remove(it) != null
            mapDetachedPlaybackToTime.remove(it)
            if (toInActive) it.onInActive()
            playbackDispatcher.onPlaybackRemoved(it)
            it.onRemoved()
            it.onDestroyed()
            containerProvider.provideLifecycleOwner()
                .lifecycle.removeObserver(it)
          }
    }
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  internal open fun onTargetActive(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapAttachedPlaybackToTime[it] = System.nanoTime()
      mapDetachedPlaybackToTime.remove(it)
      if (kohii.mapPlayableToManager[it.playable] === this) { // added 20190115, check this.
        tryRestorePlaybackInfo(it)
        if (it.token?.shouldPrepare() == true) it.prepare()
        it.onActive()
      }
      this@PlaybackManager.dispatchRefreshAll()
    } ?: throw IllegalStateException(
        "No Playback found for Target: $target in Manager: $this"
    )
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  // Call this will also save old PlaybackInfo if needed.
  internal open fun onTargetInActive(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapDetachedPlaybackToTime[it] = System.nanoTime() // Mark as detached/inactive
      // Call before this@Manager.dispatchRefreshAll()
      mapAttachedPlaybackToTime.remove(it)
          ?.run { it.onInActive() }
      this@PlaybackManager.dispatchRefreshAll() // To refresh latest playback status.
      if (kohii.mapPlayableToManager[it.playable] === this) {
        // Only pause and release if this Manager manages the Playable.
        it.pause()
        trySavePlaybackInfo(it)
        it.release()
      }
    }
  }

  internal fun onTargetUpdated(target: Any) {
    val playback = mapTargetToPlayback[target]
    if (playback != null) {
      this.containers.firstOrNull { it.allowsToPlay(playback) }
          ?.also {
            this.dispatchRefreshAll()
          }
    }
  }

  internal fun findSuitableContainer(target: Any): Container? {
    return this.containers.firstOrNull { it.accepts(target) }
  }

  internal fun findPlaybackForTarget(target: Any?) = this.mapTargetToPlayback[target]

  /// [END] Internal API

}