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

import androidx.annotation.CallSuper
import androidx.collection.ArraySet
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
 *
 * A [PlaybackManager]
 */
abstract class PlaybackManager(
  val kohii: Kohii,
  internal val parent: PlaybackManagerGroup,
  internal val provider: Any
) : LifecycleObserver, Comparable<PlaybackManager> {

  companion object {
    val PRESENT = Any() // Use for mapping.
    val priorityComparator =
      Comparator<Playback<*>> { o1, o2 -> o1.config.priority.compareTo(o2.config.priority) }

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
  private val commonTargetHosts = LinkedHashSet<TargetHost>()
  // TODO in 1.0, we do not use sticky targetHost yet. It is complicated to implement, yet not so much extra benefit.
  private val stickyTargetHosts by lazy { LinkedHashSet<TargetHost>() }

  private val attachedPlaybacks = ArraySet<Playback<*>>()
  // Weak map, so detached Playback can be cleared if not referred anymore.
  // TODO need a mechanism to release those weak Playbacks.
  private val detachedPlaybacks = WeakHashMap<Playback<*>, Any>()
  // Any Playback to be removed will be put here.
  // TODO consider to remove the Playback **after** adding new one, like Activity lifecycle.
  private val playbacksToRemove = ArraySet<Playback<*>>()

  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any /* Target Container */, Playback<*>>()

  // will be updated every time a new TargetHost is registered.
  internal val targetHosts = LinkedHashSet<TargetHost>()
  // Flag that is when false, no Playback should be played in any situation.
  internal val lock = AtomicBoolean(false)
  internal var volumeInfo = VolumeInfo()

  // [BEGIN] Internal API

  @CallSuper
  @OnLifecycleEvent(ON_CREATE)
  protected open fun onOwnerCreate(owner: LifecycleOwner) {
    this.onAttached()
  }

  @CallSuper
  @OnLifecycleEvent(ON_START)
  protected open fun onOwnerStart(owner: LifecycleOwner) {
    attachedPlaybacks.forEach {
      if (kohii.mapPlayableToManager[it.playable] == null) {
        kohii.mapPlayableToManager[it.playable] = this
        parent.tryRestorePlaybackInfo(it)
        it.onActive()
        if (it.token.shouldPrepare()) it.playable.prepare()
      }
    }

    this.dispatchRefreshAll()
  }

  @CallSuper
  @OnLifecycleEvent(ON_STOP)
  protected open fun onOwnerStop(owner: LifecycleOwner) {
    val configChange = parent.activity.isChangingConfigurations
    attachedPlaybacks.forEach {
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
    if (kohii.managers.isEmpty) {
      if (!configChange) kohii.cleanUp()
    }
  }

  protected open fun onAttached() {
    // no-ops
  }

  protected open fun onDetached() {
    // no-ops
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

  fun registerTargetHost(builder: TargetHost.Builder): TargetHost? {
    val targetHost = builder.build(this)
    return this.registerTargetHost(
        checkNotNull(targetHost) { "Failed to build TargetHost from $builder" },
        builder.volumeInfo ?: this.volumeInfo
    )
  }

  // Return the targetHost if it is added and the collections are updated, null otherwise.
  internal fun registerTargetHost(
    targetHost: TargetHost,
    volumeInfo: VolumeInfo = this.volumeInfo,
    sticky: Boolean = false
  ): TargetHost? {
    val added =
      if (sticky) {
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
    if (added) {
      targetHost.volumeInfo = volumeInfo
      targetHost.onAdded()
      targetHosts.clear()
      targetHosts.addAll(this.stickyTargetHosts + this.commonTargetHosts)
    }
    return if (added) targetHost else null
  }

  // TODO why this method is public before?
  internal fun dispatchRefreshAll() {
    this.parent.onManagerRefresh()
  }

  // Return list of all active Playbacks.
  internal fun refreshPlaybacks(): ArrayList<Playback<*>> {
    // Confirm if any invisible view is visible again.
    // This is the case of NestedScrollView.
    val toActive = detachedPlaybacks.filter { it.key.token.shouldPrepare() }
        .keys
    val toInActive = attachedPlaybacks.filter {
      val controller = it.controller
      val shouldPrepare = it.token.shouldPrepare()
      return@filter !shouldPrepare &&
          (controller == null ||
              controller.pauseBySystem() ||
              kohii.manualPlayableState[it.playable] != true)
      // TODO consider to force: if controller.startBySystem is true, then controller.pauseBySystem will be ignored.
    }

    toActive.forEach { this.onTargetActive(it.target) }
    toInActive.forEach { this.onTargetInActive(it.target) }
    return ArrayList(attachedPlaybacks)
  }

  // Return the Pair of Playbacks for play to Playbacks for pause.
  internal fun partitionPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    val playbacks = refreshPlaybacks()
    val toPlay = HashSet<Playback<*>>()

    val mapHostToCandidates = playbacks.filter { it.targetHost.allowsToPlay(it) }
        .groupBy { it.targetHost }

    // Iterate by this Set to preserve the TargetHost's order.
    targetHosts.filter { mapHostToCandidates[it] != null }
        // First TargetHost returns non empty collection will be picked
        // Use this customized extension fun so we don't need to call select for all TargetHosts
        .takeFirstOrNull(
            transformer = {
              if (it.lock.get()) {
                emptyList()
              } else {
                it.select(mapHostToCandidates.getValue(it))
              }
            },
            predicate = { it.isNotEmpty() }
        )
        ?.also {
          toPlay.addAll(it)
          playbacks.removeAll(it)
        }

    playbacks.addAll(detachedPlaybacks.keys)
    return if (lock.get()) {
      Pair(emptyList(), toPlay + playbacks)
    } else {
      Pair(toPlay.sortedWith(priorityComparator), playbacks)
    }
  }

  internal fun <CONTAINER : Any, OUTPUT : Any> performBindPlayable(
    playable: Playable<OUTPUT>,
    target: Target<CONTAINER, OUTPUT>,
    config: Config,
    creator: PlaybackCreator<OUTPUT>
  ): Playback<OUTPUT> {
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
    val ref = this.findPlaybackForPlayable(playable)
    val candidate =
      if (ref?.manager === this && ref.target === target.requireContainer() && ref.config == config) {
        ref
      } else {
        creator.createPlayback(this, target, playable, config)
            .also {
              if (config.callback != null) it.addCallback(config.callback)
              it.onCreated()
            }
      }

    val result = performAddPlayback(candidate)
    if (candidate !== ref) {
      ref?.also { it.manager.performRemovePlayback(it) }
    }

    return result
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [PlaybackManager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  internal fun <OUTPUT : Any> performAddPlayback(playback: Playback<OUTPUT>): Playback<OUTPUT> {
    val target = playback.target
    val cache = mapTargetToPlayback[target]
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
    if (attachedPlaybacks.contains(playback)) {
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
  // - attachedPlaybacks
  // - detachedPlaybacks
  // - mapPlayableTagToInfo
  internal fun performRemovePlayback(playback: Playback<*>) {
    if (mapTargetToPlayback.containsValue(playback)) {
      mapTargetToPlayback.remove(playback.target)
          ?.also {
            if (it !== playback) throw IllegalStateException(
                "Illegal cache: expect $playback but get $it"
            )
            val toInActive = attachedPlaybacks.remove(it)
            detachedPlaybacks.remove(it)
            if (toInActive) it.onInActive()
            it.onRemoved()
            it.onDestroyed()
            it.removeCallback(parent)
          }
    }
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  internal fun <T> onTargetActive(target: T) {
    mapTargetToPlayback[target as Any]?.also {
      attachedPlaybacks.add(it)
      detachedPlaybacks.remove(it)
      if (kohii.mapPlayableToManager[it.playable] === this) { // added 20190115, check this.
        parent.tryRestorePlaybackInfo(it)
        if (it.token.shouldPrepare()) it.playable.prepare()
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
    mapTargetToPlayback[target as Any]?.also {
      detachedPlaybacks[it] = PRESENT // Mark as detached/inactive
      if (attachedPlaybacks.remove(it)) it.onInActive()
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
      val properHost = this.targetHosts.firstOrNull { it.accepts(target) }
      if (properHost != null && playback.targetHost !== properHost) {
        playback.targetHost.detachTarget(target)
        playback.targetHost = properHost
        playback.targetHost.attachTarget(target)
      }
      if (playback.token.shouldPrepare()) playback.playable.prepare()
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
    }
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <OUTPUT : Any> findPlaybackForPlayable(playable: Playable<OUTPUT>): Playback<OUTPUT>? =
    this.mapTargetToPlayback.values.firstOrNull {
      it.playable === playable // this will also guaranty the type check.
    } as? Playback<OUTPUT>?

  @Suppress("UNCHECKED_CAST")
  internal fun <OUTPUT : Any> findPlaybackForOutputHolder(output: OUTPUT): Playback<OUTPUT>? =
    this.mapTargetToPlayback.values.firstOrNull { it.outputHolder === output } as? Playback<OUTPUT>

  internal fun findHostForContainer(container: Any) =
    targetHosts.firstOrNull { it.accepts(container) }

  override fun toString(): String {
    return "Manager:${Integer.toHexString(super.hashCode())}, Provider: $provider"
  }

  // [END] Internal API

  // [BEGIN] Public API

  /**
   * Apply a specific [VolumeInfo] to all Playbacks in a [Scope].
   * - The smaller a scope's priority is, the wider applicable range it will be.
   * - Applying new [VolumeInfo] to smaller [Scope] will change [VolumeInfo] of Playbacks in that [Scope].
   * - If the [Scope] is from [Scope.HOST], any new [Playback] added to that [TargetHost] will be configured
   * with the updated [VolumeInfo].
   *
   * @param receiver is the target to apply new [VolumeInfo] to. This must be set together with the [Scope].
   * For example, if client wants to apply the [VolumeInfo] to [Scope.PLAYBACK], the receiver must be the [Playback]
   * to apply to. If client wants to apply to [Scope.HOST], the receiver must be either the [Playback] inside that [TargetHost],
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
        (receiver as? Playback<*> ?: throw IllegalArgumentException(
            "Scope: $scope, expect Playback instance, found ${receiver?.javaClass}"
        )).also {
          it.volumeInfo = volumeInfo
        }
      }
      // Change volume of all Playback in the same TargetHost.
      // receiver must be a Playback, or the targetHost's object (eg: View contains the Targets).
      scope === Scope.HOST -> {
        val targetHost = when (receiver) {
          is TargetHost -> receiver
          is Playback<*> -> receiver.targetHost
          else -> targetHosts.firstOrNull { it.host === receiver }
        }
        targetHost?.also {
          it.volumeInfo = volumeInfo // all newly bound Playback will have this VolumeInfo
          this.mapTargetToPlayback.filter { pk -> pk.value.targetHost === it }
              .forEach { entry ->
                entry.value.volumeInfo = volumeInfo
              }
        }
      }
      scope === Scope.MANAGER -> {
        this.volumeInfo = volumeInfo
        targetHosts.forEach { it.volumeInfo = volumeInfo }
        for ((_, playback) in this.mapTargetToPlayback) {
          playback.volumeInfo = volumeInfo
        }
      }
      scope === Scope.ACTIVITY -> {
        this.parent.volumeInfo = volumeInfo
        this.parent.managers()
            .forEach { it.applyVolumeInfo(volumeInfo, it, Scope.MANAGER) }
      }
      scope === Scope.GLOBAL -> {
        for ((_, managerGroup) in this.kohii.groups) {
          managerGroup.volumeInfo = volumeInfo
          managerGroup.managers()
              .forEach { it.applyVolumeInfo(volumeInfo, it, Scope.MANAGER) }
        }
      }
    }
  }

  fun play(playback: Playback<*>) {
    kohii.play(playback)
  }

  fun pause(playback: Playback<*>) {
    kohii.pause(playback)
  }

  fun <CONTAINER : Any, OUTPUT : Any> registerOutputHolderPool(
    key: Pair<Class<CONTAINER>, Class<OUTPUT>>,
    outputHolderPool: OutputHolderPool<CONTAINER, OUTPUT>
  ) {
    parent.outputHolderNest.put(key, outputHolderPool)
        ?.cleanUp()
  }

  fun <CONTAINER : Any, OUTPUT : Any> fetchOutputHolderPool(
    key: Pair<Class<CONTAINER>, Class<OUTPUT>>
  ): OutputHolderPool<CONTAINER, OUTPUT>? {
    @Suppress("UNCHECKED_CAST")
    return parent.outputHolderNest[key] as OutputHolderPool<CONTAINER, OUTPUT>?
  }
}
