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
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Builder
import kohii.v1.Playback
import kohii.v1.Playback.Callback
import kohii.v1.Playback.InternalCallback
import kohii.v1.Playback.Priority
import kohii.v1.PlayerEventListener
import kohii.v1.ViewPlayback

/**
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
class ExoPlayable internal constructor(
  val kohii: Kohii,
  val media: Media,
  val builder: Builder
) : Playable, Callback, InternalCallback {

  companion object {
    private const val TAG = "Kohii:Playable"
  }

  private var playback: Playback<*>? = null

  private var listener: PlayerEventListener? = null
  private val bridge = kohii.bridgeProvider.provideBridge(builder)
      .also {
        it.repeatMode = builder.repeatMode
        it.parameters = builder.playbackParameters
        it.playbackInfo = builder.playbackInfo
      }

  // Playback.InternalCallback#onAdded(Playback)
  override fun onAdded(playback: Playback<*>) {
    require(this.playback == null || this.playback === playback) { "Bad state of playback." }
    this.playback = playback
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

  // Playback.Callback#onActive(Playback)
  override fun onActive(
    playback: Playback<*>,
    target: Any?
  ) {
    (target as? PlayerView)?.let { bridge.playerView = it }
  }

  // Playback.Callback#onInActive(Playback)
  override fun onInActive(
    playback: Playback<*>,
    target: Any?
  ) {
    // Make sure that the current Manager of this Playable is the same with playback's one, or null.
    if (kohii.mapWeakPlayableToManager[this] === playback.manager || //
        kohii.mapWeakPlayableToManager[this] == null
    ) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      if (this.bridge.playerView === target) this.bridge.playerView = null
    }
  }

  // Playback.InternalCallback#onRemoved(Playback)
  override fun onRemoved(playback: Playback<*>) {
    this.bridge.removeVolumeChangeListener(playback.volumeListeners)
    this.bridge.removeEventListener(playback.playerListeners)
    this.bridge.removeErrorListener(playback.errorListeners)
    if (this.listener != null) {
      this.bridge.removeEventListener(this.listener!!)
      this.listener = null
    }
    // Note|eneim|20190113: only call release when there is no more Manager manage this.
    if (kohii.mapWeakPlayableToManager[this] == null) {
      playback.release()
      // There is no other Manager to manage this Playable, and we are removing the last one, so ...
      kohii.releasePlayable(this.builder.tag, this)
    }
    if (this.playback === playback) this.playback = null
    playback.internalCallback = null
    playback.removeCallback(this)
  }

  ////

  override val tag: Any
    get() = builder.tag ?: Playable.NO_TAG

  // When binding to a PlayerView, any old Playback for the same PlayerView should be destroyed.
  // Relationship: [Playable] --> [Playback [Target]]
  override fun bind(target: PlayerView, @Priority priority: Int): Playback<PlayerView> {
    // Find the manager for this Playable, or create new.
    val manager = kohii.requireManager(target.context)

    // Find Playbacks those are bound to this target. If found, deactivate them.
    kohii.mapWeakWindowToManager.values.mapNotNull { it.findPlaybackForTarget(target) }
        .filter { it.playable !== this@ExoPlayable || it.manager !== manager }
        // These Playbacks were results of other Playables bound to the same Target.
        .forEach { pk -> pk.manager.onTargetInActive(target) }

    // Put this Playable to the Manager, make sure only one Manager will be managing it.
    kohii.mapWeakPlayableToManager.put(this, manager)
        ?.let {
          if (it !== manager) { // Old Manager is not the required one.
            this.playback?.let { pk ->
              if (pk.manager === it) it.performDestroyPlayback(pk)
            }
          }
        }

    @Suppress("UNCHECKED_CAST")
    val candidate = (this.playback as? Playback<PlayerView>)?.let {
      // There is old Playback
      if (it.manager === manager) { // Old Playback is in the same Manager.
        if (it.target === target) { // Old Playback is for the same Target.
          // Scenario: Rebind to same Target. (Eg: RecyclerView ViewHolder detach then re-attach)

          // Check if: there is other living Playback for the same Target.
          // ⬇: Destroy such Playback. Postpone here, manager.performAddPlayback will do later.
          // val temp = manager.mapTargetToPlayback[target]
          // if (temp?.playable != this) { // Playable bound to 'temp' is not this -> destroy it.
          //   manager.performDestroyPlayback(temp!!)
          // }

          return@let it // Reuse
        } else {
          // Scenario: Switch Target in the same Manager (Eg: Open Single Player in Fragment)
          manager.performDestroyPlayback(it) // Destroy old Playback (of old Target)
          return@let ViewPlayback(kohii, this, manager, target, priority) { builder.delay }
        }
      } else {
        // Old Playback in different Managers
        // Scenario: Switching target in different Managers (Eg: Open Single Player in Dialog)
        it.manager.performDestroyPlayback(it)
        return@let ViewPlayback(kohii, this, manager, target, priority) { builder.delay }
      }
    } ?: ViewPlayback(
        kohii,
        this,
        manager,
        target,
        priority
    ) { builder.delay } /* No old Playback, Scenario: First time binding */

    if (candidate != this.playback) {
      candidate.also {
        it.onCreated()
        it.addCallback(this)
        it.internalCallback = this
      }
    }

    return manager.performAddPlayback(candidate)
  }

  override fun prepare() {
    this.bridge.prepare(this.builder.prefetch)
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
    val secondPart = "${bridge.javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    return "$firstPart::$secondPart"
  }
}