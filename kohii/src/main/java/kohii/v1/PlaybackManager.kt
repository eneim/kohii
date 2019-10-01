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
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kohii.media.VolumeInfo
import kohii.v1.Playback.Config
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Definition of an object that can manage multiple [Playback]s.
 *
 * A [PlaybackManager]
 */
abstract class PlaybackManager(
  val kohii: Kohii,
  val provider: Any,
  internal val parent: PlaybackManagerGroup,
  internal val owner: LifecycleOwner
) : LifecycleObserver, PlayableManager, Comparable<PlaybackManager> {

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

  private val playbackStates = mutableMapOf<Playback<*>, Boolean>()
  // As a target has no idea which Playable it is bound to, Manager need to manage the link
  // So that when adding new link, it can effectively clean up old links.
  private val mapTargetToPlayback = HashMap<Any /* Target Container */, Playback<*>>()

  internal val selectionCallbacks = lazy(NONE) { OnSelectionCallbacks() }

  // will be updated every time a new TargetHost is registered.
  internal val targetHosts = LinkedHashSet<TargetHost>()
  // Flag that is when false, no Playback should be played in any situation.
  internal val lock = AtomicBoolean(false)
  internal var volumeInfo = VolumeInfo()

  // [BEGIN] Internal API

  @CallSuper
  @OnLifecycleEvent(ON_START)
  protected open fun onOwnerStart(owner: LifecycleOwner) {
    playbackStates.keys.forEach {
      val playbackActive = it.token.shouldPrepare()
      if (playbackActive) onActive(it)
      val playableManager = kohii.mapPlayableToManager[it.playable]
      // If the PlayableManager of a Playable is undefined, or is Kohii itself (= the Playable is
      // involved in a background playback), this PlayableManager will takeover it.
      // Scenario: when an Activity becomes temporarily inactive by an onStop call, if possible
      // Kohii will keep the Playable alive by background playback. Without the destruction, when
      // the Activity becomes active again by an onStart call, it will take the Playable back.
      if (playableManager == null /* no manager found */ || playableManager === kohii /* background playback */) {
        kohii.mapPlayableToManager[it.playable] = this
      }

      if (kohii.mapPlayableToManager[it.playable] === this) {
        kohii.tryRestorePlaybackInfo(it.playable)
        if (playbackActive) it.playable.prepare()
      }
    }

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
      // check if the Playback is currently playing right before this event.
      val removed = parent.selection.remove(it)
      val playable = it.playable
      // Only pause this playback if
      // - [1] config change is not happening and
      // - [2] the playable is managed by this manager, or by no-one.
      // Note: The Playable instances holds the actual playback resource. It is not managed by
      // anything else when the Activity is destroyed and to be recreated (config change).
      if (!configChange && kohii.mapPlayableToManager[playable] === this) {
        val params = playable.config.headlessPlaybackParams
        if (playable.isPlaying() && removed && params != null && params.enabled) {
          kohii.enterHeadlessPlayback(it, params)
        } else {
          kohii.trySavePlaybackInfo(playable)
          it.pauseInternal()
          it.release()
        }
        // it.release()
        // There is no recreation. If this manager is managing the playable, unload the Playable.
        // kohii.mapPlayableToManager[playable] = null
      }
      // Force the Playback to be inactive
      onInActive(it)
    }
  }

  @CallSuper
  @OnLifecycleEvent(ON_DESTROY)
  internal open fun onOwnerDestroy(owner: LifecycleOwner) {
    val configChange = parent.activity.isChangingConfigurations
    // Wrap by an ArrayList because we also remove entry while iterating by performRemovePlayback
    ArrayList(mapTargetToPlayback.values).onEach {
      if (!configChange && kohii.mapPlayableToManager[it.playable] === this@PlaybackManager) {
        kohii.mapPlayableToManager.remove(it.playable)
      }
      performRemovePlayback(it)
    }
        .clear()

    kohii.mapPlayableToManager.filter { it.value === this }
        .forEach {
          kohii.mapPlayableToManager.remove(it.key)
        }

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
      targetHost.volumeInfo = volumeInfo
      targetHost.onAdded()
      targetHosts.clear()
      targetHosts.addAll(this.stickyTargetHosts + this.commonTargetHosts)
    }
    return if (added) targetHost else null
  }

  internal fun dispatchRefreshAll() {
    this.parent.onManagerRefresh()
  }

  // Return list of all active Playbacks.
  internal fun refreshPlaybacks(): ArrayList<Playback<*>> {
    // Confirm if any invisible view is visible again.
    // This is the case of NestedScrollView.
    val toActive = playbackStates.filterNot { it.value }
        .filter { it.key.token.shouldPrepare() }
        .keys
    val toInActive = playbackStates.filterValues { it }
        .filterKeys {
          val controller = it.controller
          val shouldPrepare = it.token.shouldPrepare()
          return@filterKeys !shouldPrepare &&
              (controller == null ||
                  controller.pauseBySystem() ||
                  kohii.manualPlayableRecord[it.playable] != true)
          // TODO consider to force: if controller.startBySystem is true, then controller.pauseBySystem will be ignored.
        }

    toActive.forEach { onActive(it) }
    toInActive.forEach { onInActive(it.key) }
    return playbackStates.asSequence()
        .filter { it.value }
        .mapTo(arrayListOf(), { e -> e.key })
  }

  // Return the Pair of Playbacks to play to Playbacks to pause.
  internal fun partitionPlaybacks(): Pair<Collection<Playback<*>> /* toPlay */, Collection<Playback<*>> /* toPause */> {
    val playbacks = refreshPlaybacks()
    val toPlay = HashSet<Playback<*>>()

    val mapHostToCandidates = playbacks.filter { it.targetHost.allowsToPlay(it) }
        .groupBy { it.targetHost }

    // Iterate by this Set to preserve the TargetHost's order.
    targetHosts.asSequence()
        .filter { mapHostToCandidates[it] != null }
        .map {
          if (it.lock) emptyList() else it.select(mapHostToCandidates.getValue(it))
        }
        .firstOrNull { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          playbacks.removeAll(it)
        }

    playbacks.addAll(playbackStates.filterValues { !it }.keys)
    return if (lock.get()) {
      Pair(emptyList(), toPlay + playbacks)
    } else {
      Pair(toPlay, playbacks)
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
  internal fun <CONTAINER : Any, RENDERER : Any> performBindPlayable(
    playable: Playable<RENDERER>,
    target: Target<CONTAINER, RENDERER>,
    config: Config,
    creator: PlaybackCreator<RENDERER>
  ): Playback<RENDERER> {
    // First, add to global cache.
    kohii.mapPlayableToManager[playable] = this

    // Find Playback that was bound to this Playable and Target before in the same Manager.
    val existing = this.findPlaybackForPlayable(playable)
    val candidate =
      if (existing != null && existing.target === target.container && existing.config == config /* equals */) {
        existing
      } else {
        creator.createPlayback(this, target, playable, config)
            .also {
              if (config.callback != null) it.addCallback(config.callback)
            }
      }

    val (added, removed) = performAddPlayback(candidate)
    kohii.tryRestorePlaybackInfo(added.playable)

    // Next, search for and remove any other binding of same Target/Playable as well.
    val toRemove = lazy(NONE) { mutableSetOf<Playback<*>>() }
    if (candidate !== existing) {
      if (existing != null && existing !== removed) { // 'removed' Playback is removed already.
        toRemove.value += existing
      }
    }

    val others = kohii.managers.filter { it.value !== this }

    // [All] = [Same Target] + [Same Playable] - [Both] + [Others] ([Both] was counted twice)
    val playbacksInOtherManagers = others.flatMap { it.value.mapTargetToPlayback.values } // [All]
    val sameTarget = others.mapNotNull { it.value.mapTargetToPlayback[target.container] }
    val samePlayable = (playbacksInOtherManagers - sameTarget).filter {
      it.playable === playable
    } // [Same Playable] - [Both]

    // Only care about [Same Target] + [Same Playable] - [Both]
    (sameTarget + samePlayable).also {
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
  internal fun <OUTPUT : Any> performAddPlayback(playback: Playback<OUTPUT>): Pair<Playback<OUTPUT>, Playback<*>?> {
    val target = playback.target
    // cache = old Playback of same target --> its Playable should also be cleared.
    val cache = mapTargetToPlayback[target]
    val shouldAdd = cache == null || cache !== playback

    if (shouldAdd) {
      playback.addCallback(parent)
      // State: there is another Playback of the same Target in the same Manager.
      // Scenario: RecyclerView recycle VH so one Target can be rebound to other Playback.
      // Action: destroy the other Playback, then add the new one.
      if (cache != null) {
        if (cache.playable !== playback.playable && kohii.mapPlayableToManager[cache.playable] === this) {
          // The Playable of 'cache' must be removed.
          kohii.trySavePlaybackInfo(cache.playable)
          kohii.mapPlayableToManager.remove(cache.playable)
        }
        performRemovePlayback(cache)
      }
      mapTargetToPlayback[target] = playback
      onAdded(playback)
    }

    // In case we are adding existing one and it is already active, we refresh everything.
    if (playbackStates.containsKey(playback)) {
      if (playback.targetHost.allowsToPlay(playback)) this.dispatchRefreshAll()
    }

    // At this point, mapTargetToPlayback must contains the playback instance.
    if (BuildConfig.DEBUG) {
      check(mapTargetToPlayback.containsValue(playback)) { "Could not add Playback: $playback" }
    }

    return playback to cache
  }

  // Permanently remove the Playback from cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapTargetToPlayback
  private fun performRemovePlayback(playback: Playback<*>) {
    val target = playback.target
    mapTargetToPlayback.remove(target)
        ?.also {
          check(it === playback) { "Illegal cache: expect $playback but get $it" }
          if (playbackStates.containsKey(it)) {
            onInActive(it)
            onDetached(it)
            playbackStates.remove(it)
          }
          parent.selection.remove(it)
          onRemoved(it)
        }
  }

  // Called by Playback to notify that its Target has internal change.
  internal fun onTargetUpdated(target: Any) {
    val playback = mapTargetToPlayback[target]
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

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  internal fun onTargetAttached(target: Any) {
    mapTargetToPlayback[target]?.also {
      playbackStates[it] = false
      // [Q] Any chance that kohii.mapPlayableToManager[it.playable] is not 'this'?
      if (kohii.mapPlayableToManager[it.playable] === this) { // added 20190115, check this.
        kohii.tryRestorePlaybackInfo(it.playable)
        if (it.token.shouldPrepare()) it.playable.prepare()
        onAttached(it)
        onActive(it)
      }
      this@PlaybackManager.dispatchRefreshAll()
    } ?: throw IllegalStateException(
        "No Playback found for Target: $target in Manager: $this"
    )
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  // Call this will also save old PlaybackInfo if needed.
  // We need to keep track of detached target, because in case of RecyclerView, when a ViewHolder's
  // View is detached, it is still bound to data and can be re-attached anytime without any re-binding.
  internal fun onTargetDetached(target: Any) {
    val configChange = parent.activity.isChangingConfigurations
    mapTargetToPlayback[target]?.also {
      parent.selection.remove(it)
      if (playbackStates.containsKey(it)) {
        onInActive(it)
        onDetached(it)
        playbackStates.remove(it)
      }
      if (!configChange && kohii.mapPlayableToManager[it.playable] === this) {
        // Only pause and release if this Manager manages the Playable.
        kohii.trySavePlaybackInfo(it.playable)
        it.pauseInternal()
        // We need to release here. Reason: in RecyclerView, when the ViewHolder is detached,
        // its PlayerView will be inactive.
        it.release()
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
    playbackStates[playback] = true
    playback.onTargetActive()
  }

  // Place where Playback should free the Target from its container (e.g remove PlayerView).
  private fun onInActive(playback: Playback<*>) {
    playbackStates[playback] = false
    playback.onTargetInActive()
  }

  private fun onDetached(playback: Playback<*>) {
    playback.onTargetDetached()
  }

  private fun onRemoved(playback: Playback<*>) {
    playback.onRemoved()
    playback.removeCallback(parent)
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <OUTPUT : Any> findPlaybackForPlayable(playable: Playable<OUTPUT>): Playback<OUTPUT>? =
    this.mapTargetToPlayback.values.firstOrNull {
      it.playable === playable // this will also guaranty the type check.
    } as? Playback<OUTPUT>?

  @Suppress("UNCHECKED_CAST")
  internal fun <OUTPUT : Any> findPlaybackForOutput(output: OUTPUT): Playback<OUTPUT>? =
    this.mapTargetToPlayback.values.firstOrNull { it.renderer === output } as? Playback<OUTPUT>

  internal fun findHostForContainer(container: Any) =
    targetHosts.firstOrNull { it.accepts(container) }

  internal fun isActive(playback: Playback<*>): Boolean {
    return playbackStates[playback] == true
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
    onTargetDetached(playback.target)
    performRemovePlayback(playback)
  }
}
