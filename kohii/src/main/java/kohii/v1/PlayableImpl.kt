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

@Suppress("MemberVisibilityCanBePrivate")
/**
 * @author eneim (2018/06/24).
 */
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
    this.helper.addErrorListener(playback.errorListeners)
    this.helper.addEventListener(playback.playerListeners)
    this.helper.addVolumeChangeListener(playback.volumeListeners)
  }

  override fun onTargetAvailable(playback: Playback<*>) {
    (playback.getTarget() as? PlayerView)?.run { helper.playerView = this }
  }

  override fun onTargetUnAvailable(playback: Playback<*>) {
    // This will release current Video MediaCodec instances, which are expensive to retain.
    if (kohii.mapWeakPlayableToManager[this] == playback.manager) {
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
      // There is no more Manager to manage this Playable, and we are removing the last one, so ...
      kohii.releasePlayable(this.builder.tag, this)
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
    kohii.mapWeakPlayableToManager[this] = manager
    var playback: Playback<PlayerView>? = null
    val oldTarget = manager.mapPlayableToTarget.put(this, playerView)
    if (oldTarget === playerView) {
      @Suppress("UNCHECKED_CAST")
      playback = manager.mapTargetToPlayback[oldTarget] as Playback<PlayerView>?
      // Many Playbacks may share the same Target, but not share the same Playable.
      if (playback?.playable != this) playback = null
    } else {
      val oldPlayback = manager.mapTargetToPlayback.remove(oldTarget)
      if (oldPlayback != null) {
        manager.performDestroyPlayback(oldPlayback)
      }
    }

    if (playback == null) {
      playback = ViewPlayback(kohii, this, uri, manager, playerView, builder,
          Playback.DEFAULT_DISPATCHER)
      playback.onCreated()
      playback.addCallback(this)
      playback.internalCallback = this
    }

    return manager.performAddPlayback(playback)
  }

  override fun prepare() {
    this.helper.prepare(this.builder.prepareAlwaysLoad)
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
}