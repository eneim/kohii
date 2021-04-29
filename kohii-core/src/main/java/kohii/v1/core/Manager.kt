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

package kohii.v1.core

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.collection.arraySetOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.core.Scope.BUCKET
import kohii.v1.core.Scope.GLOBAL
import kohii.v1.core.Scope.GROUP
import kohii.v1.core.Scope.MANAGER
import kohii.v1.core.Scope.PLAYBACK
import kohii.v1.core.Strategy.SINGLE_PLAYER
import kohii.v1.internal.ManagerViewModel
import kohii.v1.logDebug
import kohii.v1.logInfo
import kohii.v1.media.VolumeInfo
import kohii.v1.partitionToMutableSets
import java.util.ArrayDeque

/**
 * Manager is a component designed to manage a [Fragment] or an [Activity] which contains *big
 * View*s like RecyclerView or ViewPager. Manager creates and manages Buckets for Views on demand.
 * You only need to create Bucket for the View whose manages Videos. For example, your app has
 * 2 RecyclerViews A and B, but only B contains Videos. You can ask the Manager to create and
 * manage a Bucket for the RecyclerView B only. That way, the library doesn't need to observe and
 * handle the changes of the RecyclerView A.
 *
 * Creating a Manager can be done as below:
 *
 * ```Kotlin
 * // Currently we are in the scope of a Fragment's onViewCreated.
 * val engine = // Get an Engine instance.
 * val manager = engine.register(this)
 * ```
 *
 * @param memoryMode The [MemoryMode] used in this Manager.
 * @param activeLifecycleState the minimum [Lifecycle.State] where the [Playback] can be playing.
 */
class Manager internal constructor(
  internal val master: Master,
  internal val group: Group,
  val host: Any,
  internal val lifecycleOwner: LifecycleOwner,
  internal val viewModel: ManagerViewModel,
  internal val memoryMode: MemoryMode = LOW,
  internal val activeLifecycleState: State = State.STARTED
) : PlayableManager, DefaultLifecycleObserver, LifecycleEventObserver, Comparable<Manager> {

  companion object {
    private fun compareAndCheck(
      left: Prioritized,
      right: Prioritized
    ): Int {
      val ltr = left.compareTo(right)
      val rtl = right.compareTo(left)
      check(ltr + rtl == 0) {
        "Sum of comparison result of 2 directions must be 0, get ${ltr + rtl}."
      }

      return ltr
    }
  }

  internal var lock: Boolean = group.lock
    get() = field || group.lock
    set(value) {
      field = value
      buckets.forEach { it.lock = value }
      refresh()
    }

  // Need RendererProvider to be Manager-scoped since we may have Fragment as Renderer.
  private val rendererProviders = mutableMapOf<Class<*>, RendererProvider>()
  private val playableObservers = mutableMapOf<Any, PlayableObserver>()

  // Use as both Queue and Stack.
  // - When adding new Bucket, we add it to tail of the Queue.
  // - When promoting a Bucket as sticky, we push the same Bucket to head of the Queue.
  // - When demoting a Bucket from sticky, we just poll the head.
  internal val buckets = ArrayDeque<Bucket>(4 /* less than default minimum of ArrayDeque */)

  // Up to one Bucket can be sticky at a time.
  private var stickyBucket: Bucket? = null
    set(value) {
      val prevStickyBucket = field
      field = value
      val nextStickyBucket = field
      if (prevStickyBucket === nextStickyBucket) return
      // Promote 'nextStickyBucket' from buckets.
      if (nextStickyBucket != null) {
        // Set new sticky Bucket by pushing it to head.
        buckets.push(nextStickyBucket)
      } else {
        // 'nextStickyBucket' is null then 'prevStickyBucket' must be nonnull. Consider to remove it
        // from head.
        if (buckets.peek() === prevStickyBucket) buckets.pop()
      }
    }

  internal val playbacks = mutableMapOf<Any /* container */, Playback>()

  internal var sticky: Boolean = false

  internal var managerVolumeInfo: VolumeInfo = VolumeInfo.DEFAULT_ACTIVE
    set(value) {
      field = value
      // Update VolumeInfo of all Buckets. This operation will then callback to this #applyVolumeInfo
      buckets.forEach { it.bucketVolumeInfo = value }
    }

  internal val volumeInfo: VolumeInfo
    get() = managerVolumeInfo

  init {
    managerVolumeInfo = group.volumeInfo
  }

  override fun compareTo(other: Manager): Int {
    return if (other.host !is Prioritized) {
      if (this.host is Prioritized) 1 else 0
    } else {
      if (this.host is Prioritized) {
        compareAndCheck(this.host, other.host)
      } else -1
    }
  }

  /**
   * @see [LifecycleEventObserver.onStateChanged]
   */
  override fun onStateChanged(
    source: LifecycleOwner,
    event: Event
  ) {
    for ((_, playback) in playbacks) {
      playback.lifecycleState = source.lifecycle.currentState
    }
    refresh()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    playbacks.values
        .toMutableList()
        .also { group.selection -= it }
        // Remove the playback. This will also modify 'playbacks' content.
        .onEach(::removePlayback)
        .clear()
    stickyBucket = null // will pop current sticky Bucket from the Stack

    buckets
        .toMutableList()
        .onEach { onRemoveBucket(it.root) }
        .clear()

    rendererProviders
        .onEach {
          owner.lifecycle.removeObserver(it.value)
          it.value.clear()
        }
        .clear()

    playableObservers.clear()
    owner.lifecycle.removeObserver(this)
    group.onManagerDestroyed(this)
  }

  // This will also update active/inactive Playbacks accordingly.
  override fun onStart(owner: LifecycleOwner): Unit = refresh()

  override fun onStop(owner: LifecycleOwner) {
    playbacks.forEach { if (it.value.isActive) onPlaybackInActive(it.value) }
    refresh()
  }

  override fun trySavePlaybackInfo(playable: Playable) {
    viewModel.trySavePlaybackInfo(playable)
  }

  override fun tryRestorePlaybackInfo(playable: Playable) {
    viewModel.tryRestorePlaybackInfo(playable)
  }

  internal fun findRendererProvider(playable: Playable): RendererProvider {
    val cache = rendererProviders[playable.config.rendererType]
        ?: rendererProviders.entries.firstOrNull {
          // If there is a RendererProvider of subclass, we can use it.
          playable.config.rendererType.isAssignableFrom(it.key)
        }?.value
    return requireNotNull(cache)
  }

  fun registerRendererProvider(
    type: Class<*>,
    provider: RendererProvider
  ) {
    val prev = rendererProviders.put(type, provider)
    if (prev != null && prev !== provider) {
      prev.clear()
      lifecycleOwner.lifecycle.removeObserver(prev)
    }

    lifecycleOwner.lifecycle.addObserver(provider)
  }

  internal fun isChangingConfigurations(): Boolean {
    return group.activity.isChangingConfigurations
  }

  internal fun findBucketForContainer(container: ViewGroup): Bucket? {
    if (!container.isAttachedToWindow) return null
    return buckets.firstOrNull { it.accepts(container) }
  }

  internal fun onContainerAttachedToWindow(container: Any?) {
    val playback = playbacks[container]
    "Manager#onContainerAttachedToWindow: $playback".logDebug()
    if (playback != null) {
      onPlaybackAttached(playback)
      onPlaybackActive(playback)
      refresh()
    }
  }

  internal fun onContainerDetachedFromWindow(container: Any?) {
    // A detached Container can be re-attached later (in case of RecyclerView)
    val playback = playbacks[container]
    "Manager#onContainerDetachedFromWindow: $playback".logDebug()
    if (playback != null) {
      if (playback.isAttached) {
        if (playback.isActive) onPlaybackInActive(playback)
        onPlaybackDetached(playback)
      }
      refresh()
    }
  }

  internal fun onContainerLayoutChanged(container: Any) {
    val playback = playbacks[container]
    if (playback != null) refresh()
  }

  private fun onAddBucket(
    view: View,
    strategy: Strategy,
    selector: Selector
  ) {
    val existing = buckets.find { it.root === view }
    if (existing != null) return
    val bucket = Bucket[this@Manager, view, strategy, selector]
    if (buckets.add(bucket)) {
      bucket.onAdded()
    }
  }

  private fun onRemoveBucket(view: View) {
    buckets.firstOrNull { it.root === view && buckets.remove(it) }
        ?.onRemoved()
  }

  internal fun refresh(): Unit = group.onRefresh()

  private fun refreshPlaybackStates(): Pair<MutableSet<Playback> /* Active */, MutableSet<Playback> /* InActive */> {
    val toActive = playbacks.filterValues { !it.isActive && it.token.shouldPrepare() }
        .values
    val toInActive = playbacks.filterValues { it.isActive && !it.token.shouldPrepare() }
        .values

    toActive.forEach(::onPlaybackActive)
    toInActive.forEach(::onPlaybackInActive)

    return playbacks.entries.filter { it.value.isAttached }
        .partitionToMutableSets(
            predicate = { it.value.isActive },
            transform = { it.value }
        )
  }

  /**
   * This method does the following things:
   * - Refreshes the state of all managed [Playback].
   * - Splits the [Playback]s into 2 buckets: those can be played and those can not.
   */
  internal fun splitPlaybacks(): Pair<Set<Playback> /* toPlay */, Set<Playback> /* toPause */> {
    val (activePlaybacks, inactivePlaybacks) = refreshPlaybackStates()
    val toPlay = arraySetOf<Playback>()

    val bucketToPlaybacks = playbacks.values.groupBy { it.bucket } // -> Map<Bucket, List<Playback>
    buckets.asSequence()
        .filter { !bucketToPlaybacks[it].isNullOrEmpty() }
        .map { /* Bucket --> Collection<Playback> */
          val candidates = bucketToPlaybacks
              .getValue(it)
              .filter { playback ->
                // TODO(eneim): rethink this to support off-screen manual playback/kohiiCanPause().
                /* val cannotPause = master.manuallyStartedPlayable.get() === playback.playable &&
                    master.plannedManualPlayables.contains(playback.tag) &&
                    !requireNotNull(playback.config.controller).kohiiCanPause() */
                return@filter /* cannotPause || */ it.allowToPlay(playback)
              }
          return@map it.strategy(it.selectToPlay(candidates))
        }
        .find { it.isNotEmpty() }
        ?.also {
          toPlay.addAll(it)
          activePlaybacks.removeAll(it)
        }

    val toPause = activePlaybacks.apply { addAll(inactivePlaybacks) }
    return if (lock || lifecycleOwner.lifecycle.currentState < activeLifecycleState) {
      emptySet<Playback>() to (toPlay + toPause)
    } else {
      toPlay to toPause
    }
  }

  internal fun addPlayback(playback: Playback) {
    val prev = playbacks.put(playback.container, playback)
    require(prev == null)
    playback.lifecycleState = lifecycleOwner.lifecycle.currentState
    playback.onAdded()
    playback.bucket.addContainer(playback.container)
  }

  internal fun removePlayback(playback: Playback) {
    if (playbacks.remove(playback.container) === playback) {
      if (playback.isAttached) {
        if (playback.isActive) onPlaybackInActive(playback)
        onPlaybackDetached(playback)
      }
      playback.bucket.removeContainer(playback.container)
      playback.onRemoved()
      refresh()
    }
  }

  internal fun onRemoveContainer(container: Any) {
    playbacks[container]?.let(::removePlayback)
  }

  internal fun notifyPlaybackChanged(playable: Playable, from: Playback?, to: Playback?) {
    "Manager#notifyPlaybackChanged ${playable.tag}, $from, $to, $this".logInfo()
    playableObservers[playable.tag]?.invoke(playable.tag, from, to)
  }

  override fun addPlayable(playable: Playable) = viewModel.addPlayable(playable)

  override fun removePlayable(playable: Playable) = viewModel.removePlayable(playable)

  private fun onPlaybackAttached(playback: Playback): Unit = playback.onAttached()

  private fun onPlaybackDetached(playback: Playback): Unit = playback.onDetached()

  private fun onPlaybackActive(playback: Playback): Unit = playback.onActive()

  private fun onPlaybackInActive(playback: Playback): Unit = playback.onInActive()

  // Public APIs

  /**
   * Observe to the changes of [Playable]'s [Playback] by its tag.
   *
   * @param mediaTag tag of the [Playable] to observe, must not be [Master.NO_TAG].
   * @param observer the [PlayableObserver] to be notified about the change.
   * @return `true` if the [observer] is registered, `false` otherwise.
   */
  fun observe(mediaTag: Any, observer: PlayableObserver): Boolean {
    require(mediaTag !== Master.NO_TAG)
    return playableObservers.put(mediaTag, observer) !== observer
  }

  @Deprecated("Using addBucket with single View instead.")
  fun addBucket(vararg views: View): Manager {
    views.forEach { this.onAddBucket(it, SINGLE_PLAYER, SINGLE_PLAYER) }
    return this
  }

  @JvmOverloads
  fun addBucket(
    view: View,
    strategy: Strategy = SINGLE_PLAYER,
    selector: Selector = SINGLE_PLAYER
  ): Manager {
    this.onAddBucket(view, strategy, selector)
    return this
  }

  fun removeBucket(vararg views: View): Manager {
    views.forEach { this.onRemoveBucket(it) }
    return this
  }

  internal fun stick(bucket: Bucket) {
    this.stickyBucket = bucket
  }

  // Null bucket --> unstick all current sticky buckets
  internal fun unstick(bucket: Bucket?) {
    if (bucket == null || this.stickyBucket === bucket) {
      this.stickyBucket = null
    }
  }

  internal fun onBucketVolumeInfoUpdated(
    bucket: Bucket,
    effectiveVolumeInfo: VolumeInfo
  ) = playbacks.forEach {
    if (it.value.bucket === bucket) it.value.playbackVolumeInfo = effectiveVolumeInfo
  }

  /**
   * Apply a specific [VolumeInfo] to all Playbacks in a [Scope].
   * - The smaller a scope's priority is, the wider applicable range it will be.
   * - Applying new [VolumeInfo] to smaller [Scope] will change [VolumeInfo] of Playbacks in that [Scope].
   * - If the [Scope] is from [Scope.BUCKET], any new [Playback] added to that [Bucket] will be configured
   * with the updated [VolumeInfo].
   *
   * @param target is the container to apply new [VolumeInfo] to. This must be set together with the [Scope].
   * For example, if client wants to apply the [VolumeInfo] to [Scope.PLAYBACK], the receiver must be the [Playback]
   * to apply to. If client wants to apply to [Scope.BUCKET], the receiver must be either the [Playback] inside that [Bucket],
   * or the root object of a [Bucket].
   */
  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    target: Any,
    scope: Scope
  ) {
    when (scope) {
      PLAYBACK -> {
        require(target is Playback) { "Expected Playback, found ${target.javaClass.canonicalName}" }
        target.playbackVolumeInfo = target.bucket.effectiveVolumeInfo(volumeInfo)
      }
      BUCKET -> {
        when (target) {
          is Bucket -> target.bucketVolumeInfo = volumeInfo
          is Playback -> target.bucket.bucketVolumeInfo = volumeInfo
          // If neither Playback nor Bucket, must be the root View of the Bucket.
          else -> {
            requireNotNull(buckets.find { it.root === target }) {
              "$target is not a root of any Bucket."
            }
                .bucketVolumeInfo = volumeInfo
          }
        }
      }
      MANAGER -> {
        this.managerVolumeInfo = volumeInfo
      }
      GROUP -> {
        this.group.groupVolumeInfo = volumeInfo
      }
      GLOBAL -> {
        this.master.groups.forEach { it.groupVolumeInfo = volumeInfo }
      }
    }
  }

  fun play(playable: Playable): Unit = master.play(playable)

  fun pause(playable: Playable): Unit = master.pause(playable)

  interface OnSelectionListener {

    // Called when some Playbacks under this Manager are selected.
    fun onSelection(selection: Collection<Playback>)
  }
}
