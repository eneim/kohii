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

package kohii.v1

import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.v1.Playable.Builder
import kohii.v1.exo.ExoBridge

/**
 * @author eneim (2018/06/24).
 */
@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class PlayableImpl internal constructor(
    val kohii: Kohii,
    val uri: Uri,
    val builder: Builder
) : Playable, Playback.Callback, Playback.InternalCallback {

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
  }

  override fun onTargetAvailable(playback: Playback<*>) {
    this.helper.prepare(this.builder.prepareAlwaysLoad)
    if (playback.getTarget() is PlayerView) {
      this.helper.playerView = playback.getTarget() as PlayerView
    }
  }

  override fun onTargetUnAvailable(playback: Playback<*>) {
    // This will release current Video MediaCodec instances, which are expensive to retain.
    if (kohii.mapPlayableToManager[this] == playback.manager) {
      this.helper.playerView = null
    }
  }

  override fun onRemoved(playback: Playback<*>) {
    if (this.listener != null) {
      this.helper.removeEventListener(this.listener!!)
      this.listener = null
    }
    if (kohii.mapPlayableToManager[this] == null) {
      playback.release()
      if (this.builder.tag != null) kohii.releasePlayable(this.builder.tag, this)
    }
    playback.internalCallback = null
    playback.removeCallback(this)
  }

  ////

  // When binding to a PlayerView, any old playback should not be paused. We know it should keep playing.
  // 
  // Relationship: [Playable] --> [Playback [Target]]
  override fun bind(playerView: PlayerView): Playback<PlayerView> {
    val manager = kohii.getManager(playerView.context)
    kohii.onManagerStateMayChange(manager, this, true)
    var playback: Playback<PlayerView>? = null
    val oldTarget = manager.mapPlayableToTarget.put(this, playerView)
    if (oldTarget === playerView) {
      @Suppress("UNCHECKED_CAST")
      playback = manager.mapTargetToPlayback[oldTarget] as? Playback<PlayerView> ?: null
      // Many Playbacks may share the same Target, but not share the same Playable.
      if (playback?.playable != this) playback = null
    } else {
      val oldPlayback = manager.mapTargetToPlayback.remove(oldTarget)
      if (oldPlayback != null) {
        manager.destroyPlayback(oldPlayback)
        oldPlayback.onDestroyed()
      }
    }

    if (playback == null) {
      playback = ViewPlayback(kohii, this, uri, manager, playerView, builder)
      playback.onCreated()
      playback.addCallback(this)
      playback.internalCallback = this
    }

    return manager.addPlayback(playback)
  }

  override fun prepare() {
    // TODO [20180712] consider to use this. Should be called from a Playback.
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

  override fun addVolumeChangeListener(listener: OnVolumeChangedListener) {
    this.helper.addOnVolumeChangeListener(listener)
  }

  override fun removeVolumeChangeListener(listener: OnVolumeChangedListener?) {
    this.helper.removeOnVolumeChangeListener(listener)
  }

  override fun addPlayerEventListener(listener: PlayerEventListener) {
    this.helper.addEventListener(listener)
  }

  override fun removePlayerEventListener(listener: PlayerEventListener?) {
    this.helper.removeEventListener(listener)
  }

  override var playbackInfo: PlaybackInfo
    get() = this.helper.playbackInfo
    set(value) {
      this.helper.playbackInfo = value
    }
}