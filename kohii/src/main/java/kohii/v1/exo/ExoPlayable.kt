/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.exo

import android.util.Log
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Builder
import kohii.v1.Playback
import kohii.v1.Playback.Priority
import kohii.v1.PlaybackManager
import kohii.v1.PlayerEventListener
import kohii.v1.ViewPlayback

/**
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
class ExoPlayable internal constructor(
  val kohii: Kohii,
  builder: Builder
) : Playable<PlayerView> {

  companion object {
    private const val TAG = "Kohii:Playable"
  }

  private val bridge by lazy { kohii.bridgeProvider.provideBridge(builder) }

  private val builderTag = builder.tag
  private val prefetch = builder.prefetch

  private var playback: Playback<PlayerView>? = null
  private var listener: PlayerEventListener? = null

  override val delay = builder.delay

  override fun onAdded(playback: Playback<*>) {
    if (this.listener == null) {
      this.listener = object : PlayerEventListener {
        override fun onPlayerStateChanged(
          playWhenReady: Boolean,
          playbackState: Int
        ) {
          playback.dispatchPlayerStateChanged(playWhenReady, playbackState)
        }

        override fun onRenderedFirstFrame() {
          playback.dispatchFirstFrameRendered()
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
          Log.e("Kohii:Exo", "Error: ${error?.cause}")
        }
      }.also { this.bridge.addEventListener(it) }
    }
    this.bridge.addErrorListener(playback.errorListeners)
    this.bridge.addEventListener(playback.playerListeners)
    this.bridge.addVolumeChangeListener(playback.volumeListeners)
  }

  override fun onRemoved(playback: Playback<*>) {
    this.bridge.removeVolumeChangeListener(playback.volumeListeners)
    this.bridge.removeEventListener(playback.playerListeners)
    this.bridge.removeErrorListener(playback.errorListeners)
    if (this.listener != null) {
      this.bridge.removeEventListener(this.listener)
      this.listener = null
    }
    // Note|eneim|20190113: only call release when there is no more Manager manages this.
    if (kohii.mapPlayableToManager[this] == null) {
      playback.release()
      // There is no other Manager to manage this Playable, and we are removing the last one, so ...
      kohii.releasePlayable(builderTag, this)
    }
    playback.removeCallback(this)
  }

  // Playback.Callback#onActive(Playback)
  /* Expected:
   * - Instance of this class will have member 'playback' set to the method parameter.
   * - Bridge instance will be set with correct target (PlayerView).
   */
  override fun onActive(playback: Playback<*>) {
    require(this.playback == null || this.playback === playback) {
      "Playable $this is bound to a playback: ${this.playback}, but then be bound to another one: $playback."
    }
    @Suppress("UNCHECKED_CAST")
    this.playback = playback as Playback<PlayerView>
    playback.target?.let { bridge.playerView = it }
  }

  // Playback.Callback#onInActive(Playback)
  override fun onInActive(playback: Playback<*>) {
    // Make sure that the current Manager of this Playable is the same with playback's one, or null.
    if (kohii.mapPlayableToManager[this] === playback.manager || //
        kohii.mapPlayableToManager[this] == null
    ) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      if (this.bridge.playerView === playback.target) this.bridge.playerView = null
    }
    if (this.playback === playback) this.playback = null
  }

  ////

  override val tag: Any
    get() = builderTag ?: Playable.NO_TAG

  // When binding to a PlayerView, any old Playback for the same PlayerView should be destroyed.
  // Relationship: [Playable] --> [Playback [Target]]
  override fun bind(
    provider: ContainerProvider,
    target: PlayerView,
    priority: Int
  ): Playback<PlayerView> {
    val manager = kohii.requireManager(provider)
    val candidate = buildCandidate(provider, manager, target, priority)
    return manager.performAddPlayback(candidate)
  }

  @Suppress("unused")
  override fun bind(
    provider: ContainerProvider,
    target: PlayerView,
    priority: Int,
    cb: ((Playback<PlayerView>) -> Unit)?
  ) {
    provider.provideLifecycleOwner()
        .lifecycle.addObserver(object : LifecycleObserver {
      @OnLifecycleEvent(ON_CREATE)
      fun onCreate(lifecycleOwner: LifecycleOwner) {
        val manager = kohii.requireManager(provider)
        val candidate = buildCandidate(provider, manager, target, priority)
        val result = manager.performAddPlayback(candidate)
        cb?.invoke(result)
        lifecycleOwner.lifecycle.removeObserver(this)
      }

      @OnLifecycleEvent(ON_DESTROY)
      fun onDestroy(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(this)
      }
    })
  }

  override fun prepare() {
    this.bridge.prepare(prefetch)
  }

  override fun play() {
    this.bridge.play()
  }

  override fun pause() {
    this.bridge.pause()
  }

  override fun release() {
    this.bridge.release()
  }

  override var playbackInfo: PlaybackInfo
    get() = this.bridge.playbackInfo
    set(value) {
      this.bridge.playbackInfo = value
    }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return this.bridge.setVolumeInfo(volumeInfo)
  }

  override val volumeInfo: VolumeInfo
    get() = this.bridge.volumeInfo

  override fun toString(): String {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    val secondPart = "${bridge.javaClass.simpleName}@${Integer.toHexString(bridge.hashCode())}"
    return "$firstPart::$secondPart"
  }

  private fun buildCandidate(
    provider: ContainerProvider,
    manager: PlaybackManager,
    target: PlayerView,
    @Priority priority: Int
  ): Playback<PlayerView> {
    manager.findSuitableContainer(target)
        ?: throw IllegalStateException(
            "This provider $provider has no Container that " +
                "accepts this target: $target. Kohii requires at least one."
        )

    // Find Playbacks those are bound to this Target. If found, deactivate them.
    kohii.playbackManagerCache.mapNotNull { it.value.findPlaybackForTarget(target) }
        .filter { it.playable !== this@ExoPlayable || it.manager !== manager }
        // These Playbacks were results of other Playables bound to the same Target.
        // TODO consider to destroy instead of deactivating. Use Playback.unbind() maybe.
        .forEach { it.manager.performRemovePlayback(it) }

    // Put this Playable to the cache with the Manager, make sure only one Manager will manage it.
    val oldMan = kohii.mapPlayableToManager.put(this, manager)
    if (oldMan !== manager) { // Old Manager is not the required one.
      this.playback?.let { if (it.manager === oldMan) oldMan.performRemovePlayback(it) }
    }

    val candidate = this.playback?.let {
      // There is old Playback
      if (it.manager === manager) {
        // Old Playback is in the same Manager.
        if (it.target === target) {
          // State: Old Playback is for the same Target in same Manager.
          // Scenario: Rebind to same Target in same Manager. (Eg: RecyclerView VH detach then re-attach)
          // Action: Reuse the Playback.

          // If there is other Playback for this Target in same Manager, it will be destroyed
          // by the Manager when it perform adding this Playback.

          return@let it // Reuse
        } else {
          // State: Switch/Rebind to another Target in the same Manager.
          // Scenario: Switch Target in the same Manager.
          // Action: destroy current Playback for old Target, then create new one for new Target.
          it.manager.performRemovePlayback(it)
          return@let ViewPlayback(kohii, this, manager, target, priority) { delay }
        }
      } else {
        // State: Old Playback in different Manager
        // Scenario: Switching Target in different Manager (Eg: Open Single Player in Dialog)
        // Action: Destroy current Playback then create new one for new Target in new Manager.
        it.manager.performRemovePlayback(it)
        return@let ViewPlayback(kohii, this, manager, target, priority) { delay }
      }
    } ?:
    // State: no current Playback.
    // Scenario: first time binding.
    // Action: just create a new Playback.
    ViewPlayback(
        kohii,
        this,
        manager,
        target,
        priority
    ) { delay }

    if (candidate !== this.playback) {
      candidate.also {
        it.onCreated()
        it.observe(provider.provideLifecycleOwner())
      }
    }

    return candidate
  }
}