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

import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Bridge
import kohii.v1.DefaultEventListener
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
    val uri: Uri,
    val builder: Builder
) : Playable, Callback, InternalCallback {

  companion object {
    @Suppress("unused")
    private const val TAG = "Kohii:Playable"
  }

  private val helper = ExoBridge(kohii, builder) as Bridge
  private var listener: PlayerEventListener? = null

  init {
    this.helper.playbackInfo = builder.playbackInfo
  }

  override fun onAdded(playback: Playback<*>) {
    if (this.listener == null) {
      this.listener = object : DefaultEventListener() {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
          playback.dispatchPlayerStateChanged(playWhenReady, playbackState)
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
          super.onPlayerError(error)
          Log.e("Kohii:Exo", "Error: ${error?.cause}")
        }
      }
      this.helper.addEventListener(this.listener!!)
    }
    this.helper.addErrorListener(playback.errorListeners)
    this.helper.addEventListener(playback.playerListeners)
    this.helper.addVolumeChangeListener(playback.volumeListeners)
  }

  override fun onTargetAvailable(playback: Playback<*>) {
    (playback.target as? PlayerView)?.run { helper.playerView = this }
  }

  override fun onTargetUnAvailable(playback: Playback<*>) {
    // Need to make sure that the current Manager of this Playable is the same with playback's one.
    if (kohii.mapWeakPlayableToManager[this] == playback.manager) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      this.helper.playerView = null
    }
  }

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
    playback.internalCallback = null
    playback.removeCallback(this)
  }

  ////

  // When binding to a PlayerView, any old Playback for the same PlayerView should be ignored.
  // Relationship: [Playable] --> [Playback [Target]]
  // TODO [20180803] what if this PlayerView is already bound to another Playable?
  override fun bind(playerView: PlayerView): Playback<PlayerView> {
    val manager = kohii.getManager(playerView.context)
    kohii.mapWeakPlayableToManager[this] = manager
    var playback: Playback<PlayerView>? = null
    val oldTarget = manager.mapWeakPlayableToTarget.put(this, playerView)
    if (oldTarget === playerView) { // Scenario: rebinding data in RecyclerView
      @Suppress("UNCHECKED_CAST")
      playback = manager.mapTargetToPlayback[oldTarget] as Playback<PlayerView>?
      // Many Playbacks may share the same Target, but not share the same Playable.
      // TODO [20180806] may need a proper way to destroy the playback ↓.
      if (playback?.playable != this) playback = null
    } else {
      val oldPlayback = manager.mapTargetToPlayback.remove(oldTarget)
      if (oldPlayback != null) {
        manager.performDestroyPlayback(oldPlayback)
      }
    }

    if (playback == null) {
      playback = ViewPlayback(kohii, this, uri, manager, playerView, builder)
      playback.onCreated()
      playback.addCallback(this)
      playback.internalCallback = this
    }

    return manager.performAddPlayback(playback)
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
    return javaClass.simpleName + "@" + hashCode()
  }
}