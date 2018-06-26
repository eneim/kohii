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

import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.v1.Playable.Bundle
import kohii.v1.exo.ExoHelper
import kohii.v1.exo.ExoStore

/**
 * @author eneim (2018/06/24).
 */
@Suppress("CanBeParameter")
class Playee internal constructor(
    val kohii: Kohii,
    private val store: ExoStore,
    private val bundle: Bundle
) : Playable, Playback.Callback {

  private val uri = bundle.uri
  private val options = bundle.options
  private val helper = ExoHelper(kohii, store, options) as Helper
  private var listener: PlayerEventListener? = null

  init {
    this.helper.playbackInfo = options.playbackInfo
    this.helper.prepare(this.options.prepareAlwaysLoad)
  }

  override fun onAdded(playback: Playback<*>) {
    if (this.listener == null) {
      this.listener = object : DefaultEventListener() {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
          playback.dispatchPlayerStateChanged(playWhenReady, playbackState)
        }
      }
      this.helper.addEventListener(this.listener!!)
    }
  }

  override fun onActive(playback: Playback<*>) {
    if (playback.getTarget() is PlayerView) {
      this.helper.playerView = playback.getTarget() as PlayerView
    }
  }

  override fun onInActive(playback: Playback<*>) {
    // This will release current MediaCodec instances, which are expensive to retain.
    if (playback.manager.playablesThisActiveTo.contains(this)) {
      this.helper.playerView = null
    }
  }

  override fun onRemoved(playback: Playback<*>, recreating: Boolean) {
    playback.removeCallback(this)
    if (this.listener != null) {
      this.helper.removeEventListener(this.listener!!)
      this.listener = null
    }
    if (recreating) return
    val active = kohii.managers.values.find {
      it.playablesThisActiveTo.contains(this@Playee)
    }
    if (active == null) this.release()
  }

  ////

  override fun bind(playerView: PlayerView): Playback<PlayerView> {
    val manager = kohii.getManager(playerView.context)
    val oldTarget = manager.mapPlayableToTarget.put(this, playerView)
    if (oldTarget != null) {
      val oldPlayback = manager.mapTargetToPlayback.remove(oldTarget)
      if (oldPlayback != null) {
        manager.removePlayback(oldPlayback)
      }
    }
    val playback = ViewPlayback(this, uri, manager, playerView, options)
    playback.addCallback(this)
    return manager.addPlayback(playback)
  }

  override fun play() {
    this.helper.play()
  }

  override fun pause() {
    this.helper.pause()
  }

  override fun release() {
    this.helper.release()
    kohii.playableStore.remove(this.bundle)
  }

  override fun addVolumeChangeListener(listener: OnVolumeChangedListener) {
    this.helper.addOnVolumeChangeListener(listener)
  }

  override fun removeVolumeChangeListener(listener: OnVolumeChangedListener) {
    this.helper.removeOnVolumeChangeListener(listener)
  }

  override fun setPlaybackInfo(playbackInfo: PlaybackInfo) {
    this.helper.playbackInfo = playbackInfo
  }

  override fun getPlaybackInfo(): PlaybackInfo {
    return this.helper.playbackInfo
  }

  override fun mayUpdateStatus(manager: Manager, active: Boolean) {
    if (active) {
      kohii.managers.values.forEach { it.playablesThisActiveTo.remove(this) }
      manager.playablesThisActiveTo.add(this)
    } else {
      manager.playablesThisActiveTo.remove(this)
    }
  }
}