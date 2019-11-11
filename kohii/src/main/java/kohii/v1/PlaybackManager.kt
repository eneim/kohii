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

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.VolumeInfo
import kohii.partitionToMutableSets
import kohii.v1.Playback.Config
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Object that can manage multiple [Playback]s.
 *
 * A [PlaybackManager]
 */
abstract class PlaybackManager(
  val kohii: Kohii,
  val provider: Any,
  internal val parent: PlaybackManagerGroup,
  internal val owner: LifecycleOwner
) : PlayableManager, LifecycleObserver, Comparable<PlaybackManager> {

  companion object {

    fun compareAndCheck(
      left: Prioritized,
      right: Prioritized
    ): Int {
      val ltr = left.compareTo(right)
      val rtl = right.compareTo(left)
      check(ltr + rtl == 0) {
        "Illegal comparison result. Sum of comparison result of 2 directions must be 0, get ${ltr + rtl}."
      }

      return ltr
    }
  }

  // TargetHosts work in "first come first serve" model. Only one TargetHost will be active at a time.
  private val commonTargetHosts = LinkedHashSet<TargetHost>()
  private val stickyTargetHosts by lazy(NONE) { ArraySet<TargetHost>() }

  // True -> Attached, Active, False -> Attached, InActive, Null -> Detached
  private val playbackStates = ArrayMap<Playback<*>, Boolean>()
  // As a container has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapContainerToPlayback = ArrayMap<Any /* Target Container */, Playback<*>>()

  internal val selectionCallbacks = lazy(NONE) { OnSelectionCallbacks() }

  // will be updated every time a new TargetHost is registered.
  internal val targetHosts = LinkedHashSet<TargetHost>()
  // Flag that is when false, no Playback should be played in any situation.
  internal val lock = AtomicBoolean(false)
  internal val volumeInfo = VolumeInfo()

  // [BEGIN] Internal API

  @CallSuper
  @OnLifecycleEvent(ON_START)
  protected open fun onOwnerStart(owner: LifecycleOwner) {
    playbackStates.keys.forEach {
      val playbackActive = it.token.shouldPrepare()
      if (playbackActive) onActive(it)
      val playableManager = it.playable.manager
      // If the PlayableManager of a Playable is undefined, or is Kohii itself (= the Playable is
      // involved in a background playback), this PlayableManager will takeover it.
      // Scenario: when an Activity becomes temporarily inactive by an onStop call, if possible
      // Kohii will keep the Playable alive by background playback. Without the destruction, when
      // the Activity becomes active again by an onStart call, it will take the Playable back.
      if (playableManager == null /* no manager found */ || playableManager === kohii /* background playback */) {
        it.playable.manager = this
      }

      if (it.playable.manager === this) {
        kohii.tryRestorePlaybackInfo(it.playable)
        if (playbackActive) it.playable.prepare()
      }
    }

    // TODO FIXME should we dismiss any playback even if there is no player active.
    // When an arbitrary PlaybackManager starts, any existing HeadlessPlayback must dismiss.
    kohii.getHeadlessPlayback()
        ?.dismiss()
    this.dispatchRefreshAll()
  }

  // If user click to 'Recent Apps' button, in modern Android version the stop event is triggered,
  // but not destroy event. So there is not much event triggered to the UI components, for example
  // onDetachedFromWindow will not be happened. Considering this situation, we want to keep the
  // attached Playbacks as it, so when we are back, we can still keep track of the list of active Players.
  // Note that this event is also where starting background playback should happen.
  @CallSuper
  @OnLifecycleEvent(ON_STOP)
  protected open fun onOwnerStop(owner: LifecycleOwner) {
    val configChange = parent.activity.isChangingConfigurations
    playbackStates.keys.forEach {
      // Force the Playback to be inactive
      onInActive(it)
      // check if the Playback is currently playing right before this event.
      val removed = parent.organizer.deselect(it)
      val playable = it.playable
      // Only pause this playback if
      // - [1] config change is not happening and
      // - [2] the playable is managed by this manager, or by no-one.
      // Note: The Playable instances holds the actual playback resource. It is not managed by
      // anything else when the Activity is destroyed and to be recreated (config change).
      if (!configChange && playable.manager === this) {
        val params = playable.config.headlessPlaybackParams
        if (removed && params != null && params.enabled) {
          kohii.enterHeadlessPlayback(it, params)
        } else {
          kohii.trySavePlaybackInfo(playable)
          it.pauseInternal()
          it.release()
        }
        // it.release()
        // There is no recreation. If this manager is managing the playable, unload the Playable.
        // kohii.mapPlayableToManager[playable] = null
        playable.manager = null // TODO check this
      }
    }
  }

  @CallSuper
  @OnLifecycleEvent(ON_DESTROY)
  internal open fun onOwnerDestroy(owner: LifecycleOwner) {
    val configChange = parent.activity.isChangingConfigurations
    // Wrap by an ArrayList because we also remove entry while iterating by performRemovePlayback
    ArrayList(mapContainerToPlayback.values).onEach {
      if (!configChange && it.playable.manager === this) {
        it.playable.manager = null
      }
      performRemovePlayback(it)
    }
        .clear()

    kohii.playables.filter { it.manager === this@PlaybackManager }
        .forEach { it.manager = null }

    this.commonTargetHosts.onEach { it.onRemoved() }
        .clear()
    this.stickyTargetHosts.onEach { it.onRemoved() }
        .clear()

    this.parent.detachPlaybackManager(this)

    owner.lifecycle.removeObserver(this)
    kohii.managers.remove(owner)

    if (this.selectionCallbacks.isInitialized()) this.selectionCallbacks.value.clear()

    // If this is the last Manager, and it is not a config change, clean everything.
    if (kohii.shouldCleanUp()) {
      if (!configChange) kohii.cleanUp()
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
    val added = if (sticky) {
      this.commonTargetHosts.remove(targetHost)
      this.commonTargetHosts.addAll(this.stickyTargetHosts)
      this.stickyTargetHosts.clear()
      this.stickyTargetHosts.add(targetHost)
    } else {
      this.commonTargetHosts.add(targetHost)
    }

    if (added) {
      targetHost.volumeInfo.setTo(volumeInfo)
      targetHost.onAdded()
      targetHosts.clear()
      targetHosts.addAll(this.stickyTargetHosts + this.commonTargetHosts)
    }
    return if (added) targetHost else null
  }

  internal fun dispatchRefreshAll() {
    this.parent.onManagerRefresh()
  }

  internal fun refreshPlaybackStates(): Pair<MutableSet<Playback<*>> /* active */, MutableSet<Playback<*>> /* inactive */> {
    // Confirm if any invisible view is visible again.
    // This is the case of NestedScrollView.
    val toActive = playbackStates.filterNot { it.value }
        .filter { it.key.token.shouldPrepare() }
        .keys
    val toInActive = playbackStates.filterValues { it }
        .filterKeys {
          val controller = it.controller
          val shouldActive = it.token.shouldPrepare() ||
              (controller != null && controller.kohiiCanPause() && kohii.manualPlayableRecord[it.playable] != Kohii.PENDING_PLAY)
          return@filterKeys !shouldActive
        }

    toActive.forEach { onActive(it) }
    toInActive.forEach { onInActive(it.key) }

    return playbackStates.entries.partitionToMutableSets(
        predicate = { it.value },
        transform = { it.key }
    )
  }

  // Return the Pair of Playbacks to play to Playbacks to pause.
  internal fun partitionPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    val (activePlaybacks, inActivePlaybacks) = refreshPlaybackStates()
    val toPlay = HashSet<Playback<*>>()

    val mapHostToCandidates = activePlaybacks.filter { it.targetHost.allowsToPlay(it) }
        .groupBy { it.targetHost }

    // Iterate by this Set to preserve the TargetHost's order.
    targetHosts.asSequence()
        .filter { mapHostToCandidates[it] != null }
        .map {
          if (it.lock) emptyList() else it.select(mapHostToCandidates.getValue(it))
        }
        .find { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          activePlaybacks.removeAll(it)
        }

    activePlaybacks.addAll(inActivePlaybacks)
    return if (lock.get()) {
      Pair(emptyList(), toPlay + activePlaybacks)
    } else {
      Pair(toPlay, activePlaybacks)
    }
  }

  // Bind the Playable for a Target in this PlaybackManager.
  // This will create Playback on demand, and add it to Manager.
  /*
    Binding Playable to a Target can happen in following scenarios:
    - New binding: binding a fresh Playable to a fresh Target.
    - Rebinding (1): rebinding a Playable from one Target to the same Target.
    - Rebinding (2): rebinding a Playable from one Target to another fresh Target (not bound).
    - Rebinding (3): binding a fresh Playable to a already-bound Target.
    - Rebinding (4): rebinding a Playable from one Target to a already-bound Target.
   */
  internal fun <CONTAINER : ViewGroup, RENDERER : Any> performBindPlayable(
    playable: Playable<RENDERER>,
    target: Target<CONTAINER, RENDERER>,
    config: Config,
    creator: PlaybackCreator<RENDERER>
  ): Playback<RENDERER> {
    // First, add to global cache.
    playable.manager = this

    // Find Playback that was bound to this Playable and Target before in the same Manager.
    val existing = this.findPlaybackForPlayable(playable)
    val candidate =
      if (existing != null && existing.container === target.container && existing.config == config /* equals */) {
        existing
      } else {
        creator.createPlayback(this, target, playable, config)
            .also {
              if (config.callback != null) it.addCallback(config.callback)
            }
      }

    val (added, removed) = performAddPlayback(candidate)
    playable.playback = added
    kohii.tryRestorePlaybackInfo(added.playable)

    // Next, search for and remove any other binding of same Container or Playable as well.
    val toRemove = lazy(NONE) { mutableSetOf<Playback<*>>() }
    if (candidate !== existing) {
      // 'existing' is for the same Playable, but neither same Container nor same Config.
      if (existing != null && existing !== removed) { // 'removed' Playback is removed already.
        toRemove.value += existing
      }
    }

    val otherManagers = kohii.managers.filterValues { it !== this }

    // [All] = [Same Target] + [Same Playable] - [Both] + [Others] ([Both] was counted twice)
    val playbacksInOtherManagers = otherManagers.flatMap { it.value.mapContainerToPlayback.values }
    val playbacksSameContainer =
      otherManagers.mapNotNull { it.value.mapContainerToPlayback[target.container] }
    val playbacksSamePlayable = (playbacksInOtherManagers - playbacksSameContainer).filter {
      it.playable === playable
    } // [Same Playable] - [Both]

    // Only care about [Same Target] + [Same Playable]
    (playbacksSameContainer + playbacksSamePlayable).also {
      if (it.isNotEmpty()) toRemove.value += it
    }

    if (toRemove.isInitialized()) {
      toRemove.value.forEach { it.manager.performRemovePlayback(it) }
    }

    return added
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [PlaybackManager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   *
   * @return [Pair] of added one and removed one (can be null).
   */
  private fun <RENDERER : Any> performAddPlayback(playback: Playback<RENDERER>): Pair<Playback<RENDERER>, Playback<*>?> {
    val container = playback.container
    // cache = old Playback of same container --> its Playable should also be cleared.
    val sameContainer = mapContainerToPlayback[container]
    val shouldAdd = sameContainer == null || sameContainer !== playback

    if (shouldAdd) {
      playback.addCallback(parent)
      // State: there is another Playback of the same Target in the same Manager.
      // Scenario: RecyclerView recycle VH so one Target can be rebound to other Playback.
      // Action: destroy the other Playback, then add the new one.
      if (sameContainer != null) {
        if (sameContainer.playable !== playback.playable && sameContainer.playable.manager === this) {
          // The Playable of 'cache' must be removed.
          kohii.trySavePlaybackInfo(sameContainer.playable)
          sameContainer.playable.manager = null
        }
        performRemovePlayback(sameContainer)
      }
      mapContainerToPlayback[container] = playback
      onAdded(playback)
    }

    // In case we are adding existing one and it is already active, we refresh everything.
    if (playbackStates.containsKey(playback)) {
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
    }

    // At this point, mapContainerToPlayback must contains the playback instance.
    if (BuildConfig.DEBUG) {
      check(mapContainerToPlayback.containsValue(playback)) { "Could not add Playback: $playback" }
    }

    return playback to sameContainer
  }

  // Permanently remove the Playback from cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapContainerToPlayback
  private fun performRemovePlayback(playback: Playback<*>) {
    val target = playback.container
    mapContainerToPlayback.remove(target)
        ?.also {
          check(it === playback) { "Illegal cache: expect $playback but get $it" }
          if (playbackStates.containsKey(it)) {
            onInActive(it)
            onDetached(it)
            playbackStates.remove(it)
          }
          parent.organizer.deselect(it)
          onRemoved(it)
        }
  }

  // Called by Playback to notify that its Target has internal change.
  internal fun onContainerUpdated(container: Any) {
    val playback = mapContainerToPlayback[container]
    if (playback != null) {
      playback.doubleCheckHost()
      if (playback.token.shouldPrepare()) playback.playable.prepare()
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
    }
  }

  // Called when a Playback's container is attached. Eg: PlayerView is attached to window.
  internal fun onContainerAttachedToWindow(container: Any) {
    mapContainerToPlayback[container]?.also {
      playbackStates[it] = false
      // [Q] Any chance that playable.manager is not 'this'?
      if (it.playable.manager === this) { // added 20190115, check this.
        // kohii.tryRestorePlaybackInfo(it.playable)
        // if (it.token.shouldPrepare()) it.playable.prepare()
        onAttached(it)
        onActive(it)
      }
      this@PlaybackManager.dispatchRefreshAll()
    } ?: throw IllegalStateException(
        "No Playback found for Target: $container in Manager: $this"
    )
  }

  // Called when a Playback's container is detached. Eg: PlayerView is detached from window.
  // Call this will also save old PlaybackInfo if needed.
  // We need to keep track of detached container, because in case of RecyclerView, when a ViewHolder's
  // View is detached, it is still bound to data and can be re-attached anytime without any re-binding.
  internal fun onContainerDetachedFromWindow(container: Any) {
    mapContainerToPlayback[container]?.also {
      parent.organizer.deselect(it)
      if (playbackStates.containsKey(it)) {
        onInActive(it)
        onDetached(it)
        playbackStates.remove(it)
      }
    }
    this.dispatchRefreshAll() // To refresh latest playback status.
  }

  private fun onAdded(playback: Playback<*>) {
    playback.onAdded()
  }

  private fun onAttached(playback: Playback<*>) {
    playback.onTargetAttached()
  }

  // Place where Playback should setup the Target to its container (e.g add PlayerView).
  private fun onActive(playback: Playback<*>) {
    kohii.tryRestorePlaybackInfo(playback.playable)
    if (playback.token.shouldPrepare()) playback.playable.prepare()
    playbackStates[playback] = true
    playback.onActive()
  }

  // Place where Playback should free the Target from its container (e.g remove PlayerView).
  private fun onInActive(playback: Playback<*>) {
    playbackStates[playback] = false
    playback.onInActive()
    if (!parent.activity.isChangingConfigurations &&
        playback.playable.manager === this &&
        findPlaybackForPlayable(playback.playable) === playback
    ) {
      // Only pause and release if this Manager manages the Playable.
      kohii.trySavePlaybackInfo(playback.playable)
      playback.pauseInternal()
      // We need to release here. Reason: in RecyclerView, when the ViewHolder is detached,
      // its PlayerView will be inactive.
      playback.release()
    }
  }

  private fun onDetached(playback: Playback<*>) {
    playback.onTargetDetached()
  }

  private fun onRemoved(playback: Playback<*>) {
    playback.onRemoved()
    playback.removeCallback(parent)
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <RENDERER : Any> findPlaybackForPlayable(playable: Playable<RENDERER>): Playback<RENDERER>? =
    this.mapContainerToPlayback.values.find {
      it.playable === playable // this will also guaranty the type check.
    } as? Playback<RENDERER>?

  @Suppress("UNCHECKED_CAST")
  internal fun <RENDERER : Any> findPlaybackForRenderer(renderer: RENDERER): Playback<RENDERER>? =
    this.mapContainerToPlayback.values.find { it.renderer === renderer } as? Playback<RENDERER>

  internal fun findHostForContainer(container: Any) =
    targetHosts.find { it.accepts(container) }

  internal fun isActive(playback: Playback<*>): Boolean {
    return playbackStates[playback] == true
  }

  override fun toString(): String {
    return "Manager:${Integer.toHexString(hashCode())}, Provider: ${provider.javaClass.simpleName}"
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
   * @param receiver is the container to apply new [VolumeInfo] to. This must be set together with the [Scope].
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
          else -> targetHosts.find { it.host === receiver }
        }
        targetHost?.also {
          it.volumeInfo.setTo(volumeInfo) // all newly bound Playback will have this VolumeInfo
          this.mapContainerToPlayback.filterValues { pk -> pk.targetHost === it }
              .forEach { entry ->
                entry.value.volumeInfo = volumeInfo
              }
        }
      }
      scope === Scope.MANAGER -> {
        this.volumeInfo.setTo(volumeInfo)
        targetHosts.forEach { it.volumeInfo.setTo(volumeInfo) }
        for ((_, playback) in this.mapContainerToPlayback) {
          playback.volumeInfo = volumeInfo
        }
      }
      scope === Scope.ACTIVITY -> {
        this.parent.volumeInfo.setTo(volumeInfo)
        this.parent.managers()
            .forEach { it.applyVolumeInfo(volumeInfo, it, Scope.MANAGER) }
      }
      scope === Scope.GLOBAL -> {
        for ((_, managerGroup) in this.kohii.groups) {
          managerGroup.volumeInfo.setTo(volumeInfo)
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

  fun <RENDERER : Any> registerRendererPool(
    key: Class<RENDERER>,
    rendererPool: RendererPool<RENDERER>
  ) {
    if (rendererPool is LifecycleObserver) owner.lifecycle.addObserver(rendererPool)
    parent.rendererPools.put(key, rendererPool)
        ?.cleanUp()
  }

  fun <RENDERER : Any> fetchRendererPool(
    key: Class<RENDERER>
  ): RendererPool<RENDERER>? {
    @Suppress("UNCHECKED_CAST")
    return parent.rendererPools[key] as RendererPool<RENDERER>?
  }

  fun addOnSelectionCallback(selectionCallback: OnSelectionCallback) {
    this.selectionCallbacks.value.add(selectionCallback)
  }

  @Suppress("unused") fun removeOnSelectionCallback(selectionCallback: OnSelectionCallback?) {
    this.selectionCallbacks.value.remove(selectionCallback)
  }

  internal fun promote(targetHost: TargetHost) {
    this.registerTargetHost(targetHost, sticky = true)
  }

  // TODO check this implementation.
  internal fun unbind(playback: Playback<*>) {
    onContainerDetachedFromWindow(playback.container)
    performRemovePlayback(playback)
  }
}
