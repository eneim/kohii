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
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
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
  protected val activity: Activity,
  internal val containerProvider: ContainerProvider
) : LifecycleObserver, Comparable<PlaybackManager> {

  companion object {
    val TOKEN_COMPARATOR: Comparator<Token> = Comparator { o1, o2 -> o1.compareTo(o2) }
    /* val DEFAULT_SELECTOR = object : PlaybackSelector {
      override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
        return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
      }
    } */

    fun checkComparison(
      left: Prioritized,
      right: Prioritized
    ): Int {
      val ltr = left.compareTo(right)
      val rtl = right.compareTo(left)
      if (ltr + rtl != 0) {
        throw IllegalStateException(
            "Illegal comparison result. $left to $right: $ltr, while $right to $left: $rtl."
        )
      }

      return ltr
    }
  }

  internal val containers by lazy {
    LinkedHashSet<Container>()
  }

  private val attachFlag = AtomicBoolean(false)
  private val playbackDispatcher = PlaybackDispatcher()

  private val mapAttachedPlaybackToTime = LinkedHashMap<Playback<*>, Long>()
  // Weak map, so detached Playback can be cleared if not referred anymore.
  // TODO need a mechanism to release those weak Playbacks.
  private val mapDetachedPlaybackToTime = WeakHashMap<Playback<*>, Long>()

  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any? /* Target */, Playback<*>>()

  // Candidates to hold playbacks for a refresh call.
  private val candidates = TreeMap<Token, Playback<*>>(TOKEN_COMPARATOR)

  /// [BEGIN] Internal API

  override fun compareTo(other: PlaybackManager): Int {
    return if (other.containerProvider !is Prioritized) {
      if (this.containerProvider is Prioritized) 1 else 0
    } else {
      if (this.containerProvider is Prioritized) {
        checkComparison(this.containerProvider, other.containerProvider)
      } else -1
    }
  }

  @CallSuper
  @OnLifecycleEvent(ON_CREATE)
  protected open fun onOwnerCreate(owner: LifecycleOwner) {
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
            parent.tryRestorePlaybackInfo(it)
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
    val configChange = activity.isChangingConfigurations
    mapAttachedPlaybackToTime.mapNotNull { it.key }
        .forEach {
          it.onInActive()
          val playable = it.playable
          // Only pause this playback if
          // - [1] config change is not happening and
          // - [2] the playable is managed by this manager, or by no-one.
          // FYI: The Playable instances holds the actual playback resource. It is not managed by
          // anything else when the Activity is destroyed and to be recreated (config change).
          if (!configChange && kohii.mapPlayableToManager[playable] === this) {
            it.pause()
            parent.trySavePlaybackInfo(it)
            // TODO [20190206] also release?
            it.release()
            // There is no recreation. If this manager is managing the playable, unload the Playable.
            kohii.mapPlayableToManager[playable] = null
          }
        }
  }

  @CallSuper
  @OnLifecycleEvent(ON_DESTROY)
  protected open fun onOwnerDestroy(owner: LifecycleOwner) {
    // Wrap by an ArrayList because we also remove entry while iterating by performRemovePlayback
    (ArrayList(mapTargetToPlayback.values).apply {
      this.forEach {
        performRemovePlayback(it)
        if (kohii.mapPlayableToManager[it.playable] === this@PlaybackManager) {
          kohii.mapPlayableToManager[it.playable] = null
        }
      }
    }).clear()

    this.containers.clear()

    owner.lifecycle.removeObserver(this)
    kohii.playbackManagerCache.remove(owner)
        ?.also {
          it.onDetached()
        }
    this.onDetached() // in case of failure in the lines above.
    this.parent.detachPlaybackManager(this)

    val configChange = activity.isChangingConfigurations
    // If this is the last Manager, and it is not a config change, clean everything.
    if (kohii.playbackManagerCache.isEmpty()) {
      if (!configChange) kohii.cleanUp()
    }
  }

  protected open fun onAttached() {
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
  }

  internal open fun dispatchRefreshAll() {
    this.parent.dispatchManagerRefresh()
  }

  @UiThread
  internal fun fetchPlaybackCandidates(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    candidates.clear()

    // Confirm if any invisible view is visible again.
    // THis is the case of NestedScrollView.
    val toActive = mapDetachedPlaybackToTime.filter { it.key.token?.shouldPrepare() == true }
        .keys
    val toInActive = mapAttachedPlaybackToTime.filter { it.key.token?.shouldPrepare() == false }
        .keys

    toActive.mapNotNull { it.target }
        .forEach { this.onTargetActive(it) }
    toInActive.mapNotNull { it.target }
        .forEach { this.onTargetInActive(it) }

    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    playbacks.forEach { playback ->
      val token = playback.token
      if (token != null && token.shouldPlay()) {
        candidates[token] = playback
      }
    }

    // use Container to pick Playback for playing.
    val temp = candidates.values
    val toPlay = HashSet<Playback<*>>()
    temp.groupBy { it.container }
        .map {
          it.key.select(it.value)
        }
        .forEach {
          toPlay.addAll(it)
          temp.removeAll(it)
        }

    playbacks.removeAll(toPlay)
    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    // Clean up cache
    candidates.clear()
    return Pair(toPlay, playbacks)
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
      playback.addCallback(parent)
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
            it.removeCallback(parent)
            containerProvider.provideLifecycleOwner()
                .lifecycle.removeObserver(it)
          }
    }
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  internal fun onTargetActive(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapAttachedPlaybackToTime[it] = System.nanoTime()
      mapDetachedPlaybackToTime.remove(it)
      if (kohii.mapPlayableToManager[it.playable] === this) { // added 20190115, check this.
        parent.tryRestorePlaybackInfo(it)
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
  internal fun onTargetInActive(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapDetachedPlaybackToTime[it] = System.nanoTime() // Mark as detached/inactive
      // Call before this@Manager.dispatchRefreshAll()
      mapAttachedPlaybackToTime.remove(it)
          ?.run { it.onInActive() }
      this@PlaybackManager.dispatchRefreshAll() // To refresh latest playback status.
      val configChange = activity.isChangingConfigurations
      if (!configChange && kohii.mapPlayableToManager[it.playable] === this) {
        // Become inactive in this Manager.
        // Only pause and release if this Manager manages the Playable.
        it.pause()
        parent.trySavePlaybackInfo(it)
        it.release()
      }
    }
  }

  // Called by Playback to notify that its Target has internal change.
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