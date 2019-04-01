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
import kohii.media.VolumeInfo
import kohii.takeFirstOrNull
import kohii.v1.Playback.Config
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Definition of an object that can manage multiple [Playback]s.
 */
abstract class PlaybackManager(
  val kohii: Kohii,
  val parent: ActivityContainer,
  internal val provider: LifecycleOwnerProvider
) : LifecycleObserver, Comparable<PlaybackManager> {

  companion object {
    val PRESENT = Any() // Use for mapping.
    val priorityComparator =
      Comparator<Playback<*, *>> { o1, o2 -> o1.config.priority.compareTo(o2.config.priority) }

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

  // TargetHosts work in "first come first serve" model. Only one TargetHost will be active at a time.
  private val commonTargetHosts by lazy { LinkedHashSet<TargetHost>() }
  // TODO in 1.0, we do not use sticky targetHost yet. It is complicated to implement, yet not so much extra benefit.
  private val stickyTargetHosts by lazy { LinkedHashSet<TargetHost>() }

  private val attachFlag = AtomicBoolean(false)

  private val mapAttachedPlaybackToTime = LinkedHashMap<Playback<*, *>, Any>()
  // Weak map, so detached Playback can be cleared if not referred anymore.
  // TODO need a mechanism to release those weak Playbacks.
  private val mapDetachedPlaybackToTime = WeakHashMap<Playback<*, *>, Any>()

  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any /* Target */, Playback<*, *>>()

  // [BEGIN] Internal API

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

    this.commonTargetHosts.onEach { it.onRemoved() }
        .clear()
    this.stickyTargetHosts.onEach { it.onRemoved() }
        .clear()

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
      targetHosts().forEach { it.onManagerAttached() }
    }
  }

  protected open fun onDetached() {
    if (attachFlag.compareAndSet(true, false)) {
      targetHosts().forEach { it.onManagerDetached() }
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

  internal fun registerTargetHost(
    targetHost: TargetHost,
    sticky: Boolean
  ): Boolean {
    val result = if (sticky) {
      val existing = this.commonTargetHosts.firstOrNull { it.host === targetHost.host }
      if (existing != null) {
        // remove from standard ones, add to sticky.
        this.commonTargetHosts.remove(existing)
        this.stickyTargetHosts.add(existing)
      } else {
        this.stickyTargetHosts.add(targetHost)
      }
    } else {
      this.commonTargetHosts.add(targetHost)
    }
    if (result) targetHost.onAdded()
    return result
  }

  internal fun dispatchRefreshAll() {
    this.parent.dispatchManagerRefresh()
  }

  internal fun refreshPlaybacks(): ArrayList<Playback<*, *>> {
    // Confirm if any invisible view is visible again.
    // This is the case of NestedScrollView.
    val toActive = mapDetachedPlaybackToTime.filter { it.key.token.shouldPrepare() }
        .keys
    val toInActive = mapAttachedPlaybackToTime.filter {
      val controller = it.key.controller
      val shouldPrepare = it.key.token.shouldPrepare()
      return@filter !shouldPrepare &&
          (controller == null || controller.allowsSystemControl() || kohii.manualFlag[it.key.playable] != true)
    }
        .keys

    toActive.forEach { this.onTargetActive(it.target) }
    toInActive.forEach { this.onTargetInActive(it.target) }
    return ArrayList(mapAttachedPlaybackToTime.keys)
  }

  internal fun partitionPlaybacks(): Pair<Collection<Playback<*, *>> /* toPlay */, Collection<Playback<*, *>> /* toPause */> {
    val playbacks = refreshPlaybacks()
    val toPlay = HashSet<Playback<*, *>>()

    val mapHostToPlaybacks = playbacks.filter { it.targetHost.allowsToPlay(it) }
        .groupBy { it.targetHost }

    // Iterate by this Set to preserve the TargetHost's order.
    targetHosts().filter { mapHostToPlaybacks[it] != null }
        // First TargetHost returns non empty collection will be picked
        // Use this customized extension fun so we don't need to call select for all TargetHosts
        .takeFirstOrNull(
            transformer = { it.select(mapHostToPlaybacks.getValue(it)) },
            predicate = { it.isNotEmpty() }
        )
        ?.also {
          toPlay.addAll(it)
          playbacks.removeAll(it)
        }

    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    return Pair(toPlay.sortedWith(priorityComparator), playbacks)
  }

  internal fun <TARGET, PLAYER> performBindPlayable(
    playable: Playable<PLAYER>,
    target: Target<TARGET, PLAYER>,
    config: Config,
    creator: PlaybackCreator<TARGET, PLAYER>
  ): Playback<TARGET, PLAYER> {
    // First, add to global cache.
    kohii.mapPlayableToManager[playable] = this

    // Next, search for and remove any other binding as well.
    val others = kohii.managers.filter { it.value !== this }
    // Next 1: remove old Playback that is for same Target, but different Playable and Manager
    others.mapNotNull { it.value.mapTargetToPlayback[target] }
        .filter { it.playable !== playable }
        .forEach { it.manager.performRemovePlayback(it) }

    // Next 2: remove old Playback that is for same Playable, but different Manager
    others.mapNotNull { it.value.findPlaybackForPlayable(playable) }
        .forEach {
          it.manager.performRemovePlayback(it)
        }

    // Find Playback that was bound to this Playable before in the same Manager.
    @Suppress("UNCHECKED_CAST")
    val ref = this.findPlaybackForPlayable(playable) as? Playback<TARGET, PLAYER>

    val candidate =
      if (ref?.manager === this && ref.target === target) {
        ref
      } else {
        creator.createPlayback(this, target, playable, config)
            .also { it.onCreated() }
      }

    if (candidate !== ref) {
      ref?.let { it.manager.performRemovePlayback(it) }
    }

    return performAddPlayback(candidate)
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [PlaybackManager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  internal fun <TARGET, PLAYER> performAddPlayback(playback: Playback<TARGET, PLAYER>): Playback<TARGET, PLAYER> {
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
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
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
  internal fun performRemovePlayback(playback: Playback<*, *>) {
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
      // fixed = the TargetHost that truly accepts the target.
      // Scenario: in first bind of RV, VH's itemView has null Parent, but might has LayoutParams
      // of RV, we 'temporarily' ask any RV to accept it. When it is updated, we find the correct one.
      // This operation should not happen always, ideally up to 1 time.
      val fixed = this.targetHosts()
          .firstOrNull { it.accepts(target) }
      if (fixed != null && playback.targetHost !== fixed) {
        playback.targetHost.detachTarget(target)
        playback.targetHost = fixed
        playback.targetHost.attachTarget(target)
      }
      if (playback.token.shouldPrepare()) playback.prepare()
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
    }
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <PLAYER> findPlaybackForPlayable(playable: Playable<PLAYER>): Playback<*, PLAYER>? =
    this.mapTargetToPlayback.values.firstOrNull {
      it.playable === playable // this will also guaranty the type check.
    } as? Playback<*, PLAYER>?

  @Suppress("UNCHECKED_CAST")
  fun <PLAYER : Any> findPlaybackForPlayer(player: PLAYER): Playback<*, PLAYER>? =
    this.mapTargetToPlayback.values.firstOrNull { it.playerView === player } as? Playback<*, PLAYER>

  internal fun findHostForTarget(target: Any) = targetHosts().firstOrNull { it.accepts(target) }

  private fun targetHosts(): Set<TargetHost> {
    return (stickyTargetHosts + commonTargetHosts) as MutableSet<TargetHost>
  }

  override fun toString(): String {
    return "Manager:${Integer.toHexString(super.hashCode())}, Provider: $provider"
  }

  // [END] Internal API

  // [BEGIN] Public API

  /**
   * Apply a specific [VolumeInfo] to all Playbacks in a [Scope].
   * - The smaller a scope's priority is, the wider applicable range it will be.
   * - Applying new [VolumeInfo] to smaller [Scope] will change [VolumeInfo] of Playbacks in that [Scope].
   * - If the [Scope] is from [Scope.TARGETHOST], any new [Playback] added to that [TargetHost] will be configured
   * with the updated [VolumeInfo].
   *
   * @param receiver is the target to apply new [VolumeInfo] to. This must be set together with the [Scope].
   * For example, if client wants to apply the [VolumeInfo] to [Scope.PLAYBACK], the receiver must be the [Playback]
   * to apply to. If client wants to apply to [Scope.TARGETHOST], the receiver must be either the [Playback] inside that [TargetHost],
   * or the targetHost object of a [TargetHost].
   */
  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    receiver: Any? = null,
    scope: Scope
  ) {
    when {
      // Only update VolumeInfo of current Playback.
      // Client need to update volume of more Playback should consider using higher priority Scope.
      scope === Scope.PLAYBACK -> {
        (receiver as? Playback<*, *> ?: throw IllegalArgumentException(
            "Scope: $scope, expect Playback instance, found ${receiver?.javaClass}"
        )).also {
          it.volumeInfo = volumeInfo
        }
      }
      // Change volume of all Playback in the same TargetHost.
      // receiver must be a Playback, or the targetHost's object (eg: View contains the Targets).
      scope === Scope.TARGETHOST -> {
        targetHosts().firstOrNull {
          if (receiver is Playback<*, *>) it === receiver.targetHost
          else it.host === receiver /* client must pass the targetHost View */
        }
            ?.let {
              it.volumeInfo = volumeInfo
              this.mapTargetToPlayback.filter { pk -> pk.value.targetHost === it }
                  .forEach { entry ->
                    entry.value.volumeInfo = volumeInfo
                  }
            }
      }
      scope === Scope.MANAGER -> {
        targetHosts().forEach { it.volumeInfo = volumeInfo }
        for ((_, playback) in this.mapTargetToPlayback) {
          playback.volumeInfo = volumeInfo
        }
      }
      scope === Scope.ACTIVITY ->
        this.parent.managers().forEach { it.applyVolumeInfo(volumeInfo, it, Scope.MANAGER) }
      scope === Scope.GLOBAL -> {
        for ((_, parent) in this.kohii.owners) {
          parent.managers()
              .forEach { it.applyVolumeInfo(volumeInfo, it, Scope.MANAGER) }
        }
      }
    }
  }

  fun play(playback: Playback<*, *>) {
    parent.play(playback)
  }

  fun pause(playback: Playback<*, *>) {
    parent.pause(playback)
  }
}
