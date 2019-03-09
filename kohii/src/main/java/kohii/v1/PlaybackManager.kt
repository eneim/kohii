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
import kohii.takeFirstOrNull
import kohii.v1.Playback.Options
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Definition of an object that can manage multiple [Playback]s.
 */
abstract class PlaybackManager(
  protected val kohii: Kohii,
  protected val parent: ActivityContainer,
  internal val provider: LifecycleOwnerProvider
) : LifecycleObserver, Comparable<PlaybackManager> {

  companion object {
    val PRESENT = Any() // Use for mapping.
    val priorityComparator =
      Comparator<Playback<*>> { o1, o2 -> o1.options.priority.compareTo(o2.options.priority) }

    fun compareAndCheck(
      left: Prioritized,
      right: Prioritized
    ): Int {
      val ltr = left.compareTo(right)
      val rtl = right.compareTo(left)
      if (ltr + rtl != 0) {
        throw IllegalStateException(
            "Illegal comparison result. Sum of comparison result of 2 directions must be 0, get ${ltr + rtl}."
        )
      }

      return ltr
    }
  }

  private val stickyContainers by lazy { LinkedHashSet<Container>() }
  // Containers work in "first come first serve" model. Only one Container will be active at a time.
  private val containers by lazy { LinkedHashSet<Container>() }

  private val attachFlag = AtomicBoolean(false)

  private val mapAttachedPlaybackToTime = LinkedHashMap<Playback<*>, Any>()
  // Weak map, so detached Playback can be cleared if not referred anymore.
  // TODO need a mechanism to release those weak Playbacks.
  private val mapDetachedPlaybackToTime = WeakHashMap<Playback<*>, Any>()

  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any /* Target */, Playback<*>>()

  /// [BEGIN] Internal API

  @CallSuper
  @OnLifecycleEvent(ON_CREATE)
  protected open fun onOwnerCreate(owner: LifecycleOwner) {
    Log.e("Kohii::X", "create manager: $this, owner: $provider")
    this.onAttached()
  }

  @CallSuper
  @OnLifecycleEvent(ON_START)
  protected open fun onOwnerStart(owner: LifecycleOwner) {
    mapAttachedPlaybackToTime.mapNotNull { it.key }
        .forEach {
          if (kohii.mapPlayableToManager[it.playable] == null) {
            kohii.mapPlayableToManager[it.playable] = this
            parent.tryRestorePlaybackInfo(it)
            it.onActive()
            if (it.token.shouldPrepare()) it.prepare()
          }
        }

    this.dispatchRefreshAll()
  }

  @CallSuper
  @OnLifecycleEvent(ON_STOP)
  protected open fun onOwnerStop(owner: LifecycleOwner) {
    val configChange = parent.activity.isChangingConfigurations
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
            it.release()
            // There is no recreation. If this manager is managing the playable, unload the Playable.
            kohii.mapPlayableToManager[playable] = null
          }
        }
  }

  @CallSuper
  @OnLifecycleEvent(ON_DESTROY)
  protected open fun onOwnerDestroy(owner: LifecycleOwner) {
    Log.e("Kohii::X", "destroy manager: $this, owner: $provider")
    // Wrap by an ArrayList because we also remove entry while iterating by performRemovePlayback
    (ArrayList(mapTargetToPlayback.values).apply {
      this.forEach {
        performRemovePlayback(it)
        if (kohii.mapPlayableToManager[it.playable] === this@PlaybackManager) {
          kohii.mapPlayableToManager[it.playable] = null
        }
      }
    }).clear()

    this.onDetached()
    this.containers.clear()
    this.parent.detachPlaybackManager(this)

    owner.lifecycle.removeObserver(this)
    kohii.managers.remove(owner)

    val configChange = parent.activity.isChangingConfigurations
    // If this is the last Manager, and it is not a config change, clean everything.
    if (kohii.managers.isEmpty()) {
      if (!configChange) kohii.cleanUp()
    }
  }

  protected open fun onAttached() {
    if (attachFlag.compareAndSet(false, true)) {
      containers.forEach { it.onManagerAttached() }
    }
  }

  protected open fun onDetached() {
    if (attachFlag.compareAndSet(true, false)) {
      containers.forEach { it.onManagerDetached() }
    }
  }

  override fun compareTo(other: PlaybackManager): Int {
    return if (other.provider !is Prioritized) {
      if (this.provider is Prioritized) 1 else 0
    } else {
      if (this.provider is Prioritized) {
        compareAndCheck(this.provider, other.provider)
      } else -1
    }
  }

  internal fun registerContainer(
    container: Container,
    sticky: Boolean
  ): Boolean {
    return if (sticky) {
      val existing = this.containers.firstOrNull { it.container === container.container }
      if (existing != null) {
        // remove from standard ones, add to sticky.
        this.containers.remove(existing)
        this.stickyContainers.add(existing)
      } else {
        this.stickyContainers.add(container)
      }
    } else {
      this.containers.add(container)
    }
  }

  internal fun dispatchRefreshAll() {
    this.parent.dispatchManagerRefresh()
  }

  internal fun partitionPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    // Confirm if any invisible view is visible again.
    // This is the case of NestedScrollView.
    val toActive = mapDetachedPlaybackToTime.filter { it.key.token.shouldPrepare() }
        .keys
    val toInActive = mapAttachedPlaybackToTime.filter { !it.key.token.shouldPrepare() }
        .keys

    toActive.forEach { this.onTargetActive(it.target) }
    toInActive.forEach { this.onTargetInActive(it.target) }

    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    val toPlay = HashSet<Playback<*>>()

    val grouped = playbacks.filter { it.container.allowsToPlay(it) }
        .groupBy { it.container }

    // Iterate by this Set to preserve the Containers' order.
    containers.filter { grouped[it] != null }
        // First Container returns non empty will be picked
        // Use this customized extension fun so we don't need to call select for all Containers
        .takeFirstOrNull(
            transformer = { it.select(grouped.getValue(it)) },
            predicate = { it.isNotEmpty() }
        )
        ?.also {
          toPlay.addAll(it)
          playbacks.removeAll(it)
        }

    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    return Pair(toPlay.sortedWith(priorityComparator), playbacks)
  }

  internal inline fun <reified T> performBindPlayable(
    playable: Playable<T>,
    target: T,
    options: Options,
    creator: PlaybackCreator<T>
  ): Playback<T> {
    // First, add to global cache.
    kohii.mapPlayableToManager[playable] = this

    // Next, search for and remove any other binding as well.
    val others = kohii.managers.filter { it.value !== this }
    // Next 1: remove old Playback that is for same Target, but different Playable and Manager
    others.mapNotNull { it.value.mapTargetToPlayback[target as Any] }
        .filter { it.playable !== playable }
        .forEach { it.manager.performRemovePlayback(it) }

    // Next 2: remove old Playback that is for same Playable, but different Manager
    others.mapNotNull { it.value.findPlaybackForPlayable(playable) }
        .forEach {
          it.manager.performRemovePlayback(it)
        }

    // Find Playback that was bound to this Playable before in the same Manager.
    val ref = this.findPlaybackForPlayable(playable)

    val candidate =
      if (ref != null && ref.manager === this && ref.target === target) {
        ref
      } else {
        creator.createPlayback(target, options)
      }

    if (candidate !== ref) {
      candidate.onCreated()
      ref?.let { it.manager.performRemovePlayback(it) }
    }

    return performAddPlayback(candidate)
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [PlaybackManager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  internal fun <T> performAddPlayback(playback: Playback<T>): Playback<T> {
    val target = playback.target
    val cache = mapTargetToPlayback[target as Any]
    val shouldAdd = cache == null || cache !== playback

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
      if (playback.container.allowsToPlay(playback)) this.dispatchRefreshAll()
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
      Log.w("Kohii::X", "remove: $playback, $this")
      mapTargetToPlayback.remove(playback.target)
          ?.also {
            if (it !== playback) throw IllegalStateException(
                "Illegal cache: expect $playback but get $it"
            )
            val toInActive = mapAttachedPlaybackToTime.remove(it) != null
            mapDetachedPlaybackToTime.remove(it)
            if (toInActive) it.onInActive()
            it.onRemoved()
            it.onDestroyed()
            it.removeCallback(parent)
          }
    }
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  internal fun <T> onTargetActive(target: T) {
    mapTargetToPlayback[target as Any]?.let {
      mapAttachedPlaybackToTime[it] = PRESENT
      mapDetachedPlaybackToTime.remove(it)
      if (kohii.mapPlayableToManager[it.playable] === this) { // added 20190115, check this.
        parent.tryRestorePlaybackInfo(it)
        if (it.token.shouldPrepare()) it.prepare()
        it.onActive()
      }
      this@PlaybackManager.dispatchRefreshAll()
    } ?: throw IllegalStateException(
        "No Playback found for Target: $target in Manager: $this"
    )
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  // Call this will also save old PlaybackInfo if needed.
  internal fun <T> onTargetInActive(target: T) {
    mapTargetToPlayback[target as Any]?.let {
      mapDetachedPlaybackToTime[it] = PRESENT // Mark as detached/inactive
      mapAttachedPlaybackToTime.remove(it)
          ?.run { it.onInActive() }
      this@PlaybackManager.dispatchRefreshAll() // To refresh latest playback status.
      val configChange = parent.activity.isChangingConfigurations
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
  internal fun <T> onTargetUpdated(target: T) {
    val playback = mapTargetToPlayback[target as Any]
    if (playback != null) {
      if (playback.container.allowsToPlay(playback)) this.dispatchRefreshAll()
    }
  }

  @Suppress("UNCHECKED_CAST")
  internal inline fun <reified T> findPlaybackForPlayable(playable: Playable<T>): Playback<T>? =
    this.mapTargetToPlayback.values.firstOrNull {
      it.target is T && it.playable === playable
    } as Playback<T>?

  internal fun findSuitableContainer(target: Any) =
    this.containers.firstOrNull { it.accepts(target) }

  override fun toString(): String {
    return "Manager:${Integer.toHexString(super.hashCode())}, Provider: $provider"
  }

  /// [END] Internal API

}