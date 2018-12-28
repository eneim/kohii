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
    @Suppress("unused")
    private const val TAG = "Kohii:Playable"
  }

  private var target: Any? = null
  private var playback: Playback<*>? = null

  private var listener: PlayerEventListener? = null
  private val helper = kohii.bridgeProvider.provideBridge(builder)
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
      }.also { this.helper.addEventListener(it) }
    }
    this.helper.addErrorListener(playback.errorListeners)
    this.helper.addEventListener(playback.playerListeners)
    this.helper.addVolumeChangeListener(playback.volumeListeners)
  }

  // Playback.Callback#onActive(Playback)
  override fun onActive(
    playback: Playback<*>,
    target: Any?
  ) {
    if (this.target != null && this.target !== target) {
      throw IllegalStateException("Already active for ${this.target}")
    }
    this.target = target
    (playback.target as? PlayerView)?.let { helper.playerView = it }
  }

  // Playback.Callback#onInActive(Playback)
  override fun onInActive(
    playback: Playback<*>,
    target: Any?
  ) {
    if (this.target === target) this.target = null
    // Make sure that the current Manager of this Playable is the same with playback's one, or null.
    if (kohii.mapWeakPlayableToManager[this] === playback.manager || //
        kohii.mapWeakPlayableToManager[this] == null
    ) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      this.helper.playerView = null
    }
  }

  // Playback.InternalCallback#onRemoved(Playback)
  override fun onRemoved(playback: Playback<*>) {
    this.helper.removeVolumeChangeListener(playback.volumeListeners)
    this.helper.removeEventListener(playback.playerListeners)
    this.helper.removeErrorListener(playback.errorListeners)
    if (this.listener != null) {
      this.helper.removeEventListener(this.listener!!)
      this.listener = null
    }
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
  // TODO [20180803] what if this PlayerView is already bound to another Playable?
  /* ⬇ Comment out [20181226] in favor of new implementation. Keep for testing.
  fun bindOld(playerView: PlayerView): Playback<PlayerView> {
    val manager = kohii.requireManager(playerView.context)
    // At most one Manager can manage a Playable.
    kohii.mapWeakPlayableToManager.put(this, manager)?.also {
      // There is old Manager that managed this.
      if (it != manager) { // Not the same one.
        val target = it.mapWeakPlayableToTarget.remove(this)
        val playback = it.mapTargetToPlayback[target]
        if (playback != null) it.performDestroyPlayback(playback)
      }
    }

    // Save the weak relationship, check the old one.
    val oldTarget = manager.mapWeakPlayableToTarget.put(this, playerView)
    // Is 'lazy' OK here?
    val candidate: Playback<PlayerView>? by lazy {
      when {
        oldTarget == playerView -> {
          // Bind to same target.
          // Scenario: ViewHolder is detached, recycled, and then bound again to the same Playable.
          @Suppress("UNCHECKED_CAST")
          var temp = manager.mapTargetToPlayback[oldTarget] as Playback<PlayerView>?
          // Different Playbacks of same Target cannot share the same Playable.
          if (temp?.playable != this) {
            manager.performDestroyPlayback(temp!!) // <-- maybe not perform the destroy here.
            temp = null // playback is set, but not the expected one, so make it null here.
          }
          // else { /* we can reuse this Playback */ }
          return@lazy temp
        }
        oldTarget != null -> {
          val oldPlayback = manager.mapTargetToPlayback.remove(oldTarget)
          if (oldPlayback != null) {
            manager.performDestroyPlayback(oldPlayback)
          }
          return@lazy null
        }
        else -> return@lazy null
      }
    }

    // Mutable copy.
    var result = candidate
    if (result == null) {
      result = ViewPlayback(kohii, this, manager, playerView)
      result.onCreated()
      result.addCallback(this)
      result.internalCallback = this
    }

    return manager.performAddPlayback(result)
  }
  */

  override fun bind(playerView: PlayerView): Playback<PlayerView> {
    val manager = kohii.requireManager(playerView.context)
    // Put this Playable to the Manager, make sure only one Manager will be managing it.
    kohii.mapWeakPlayableToManager.put(this, manager)
        ?.let {
          // There is old Manager that managed this.
          if (it !== manager) { // Not the same one.
            this.playback?.let { pk ->
              if (pk.manager === it) it.performDestroyPlayback(pk)
            }
          }
        }

    val candidate = this.playback?.let {
      // There is old Playback
      if (it.manager === manager) { // Old Playback in same Manager.
        if (it.target === playerView) { // Old Playback for the same Target.
          // Scenario: Rebind to same Target.

          // Check if: there is existing Playback for the same Target.
          // Below: destroy such Playback. In fact, manager.performAddPlayback will do the same.
          // val temp = manager.mapTargetToPlayback[target]
          // if (temp?.playable != this) {
          //   manager.performDestroyPlayback(temp!!)
          // }

          @Suppress("UNCHECKED_CAST")
          return@let (it as Playback<PlayerView>) // Reuse, must be of type Playback<PlayerView>
        } else {
          // Scenario: Switch Target in the same Manager
          manager.performDestroyPlayback(it) // Destroy old Playback (of old Target)
          return@let ViewPlayback(kohii, this, manager, playerView)
        }
      } else {
        // Old Playback in different Managers
        // Scenario: Switching target in different Managers
        it.manager.performDestroyPlayback(it)
        return@let ViewPlayback(kohii, this, manager, playerView)
      }
    } ?: ViewPlayback(
        kohii,
        this,
        manager,
        playerView
    ) /* No old Playback, Scenario: First time binding */

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
    this.helper.prepare(this.builder.prefetch)
  }

  override fun play() {
    this.helper.play()
  }

  override fun pause() {
    this.helper.pause()
  }

  override fun release() {
    this.helper.release()
  }

  override var playbackInfo: PlaybackInfo
    get() = this.helper.playbackInfo
    set(value) {
      this.helper.playbackInfo = value
    }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return this.helper.setVolumeInfo(volumeInfo)
  }

  override val volumeInfo: VolumeInfo
    get() = this.helper.volumeInfo

  override fun toString(): String {
    return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
  }
}