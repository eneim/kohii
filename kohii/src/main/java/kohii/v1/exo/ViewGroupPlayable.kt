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

package kohii.v1.exo

import android.util.Log
import android.view.ViewGroup
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Bridge
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Companion.NO_TAG
import kohii.v1.PlayableBinder
import kohii.v1.Playback
import kohii.v1.Playback.Config
import kohii.v1.PlaybackCreator
import kohii.v1.PlayerEventListener

class ViewGroupPlayable(
  val kohii: Kohii,
  val media: Media,
  config: Playable.Config,
  private val bridge: Bridge<PlayerView>
) : Playable<ViewGroup> {

  interface PlayerViewProvider {
    val playerView: PlayerView?
  }

  internal constructor(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ) : this(kohii, media, config, kohii.bridgeProvider.provideBridge(kohii, media, config))

  private val builderTag = config.tag
  private val prefetch = config.prefetch

  private var listener: PlayerEventListener? = null
  private var playerViewProvider: PlayerViewProvider? = null

  override fun newBinder(): PlayableBinder {
    return PlayableBinder(kohii, media)
  }

  override fun onAdded(playback: Playback<*>) {
    require(playback is PlayerViewProvider) {
      "This Playable only support Playback of type PlayerViewProvider"
    }

    playerViewProvider = playback
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
    playerViewProvider = null
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
    require(playback.target is ViewGroup) {
      "${this.javaClass.simpleName} only works with target of type ViewGroup"
    }
  }

  // Playback.Callback#onInActive(Playback)
  override fun onInActive(playback: Playback<*>) {
    // Make sure that the current Manager of this Playable is the same with playback's one, or null.
    if (kohii.mapPlayableToManager[this] === playback.manager || //
        kohii.mapPlayableToManager[this] == null
    ) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      if (this.bridge.playerView === playerViewProvider?.playerView) this.bridge.playerView = null
    }
  }

  override val tag: Any = builderTag ?: NO_TAG

  override fun prepare() {
    this.bridge.prepare(prefetch)
  }

  override fun play(playback: Playback<ViewGroup>) {
    bridge.playerView = playerViewProvider?.playerView
    bridge.play()
  }

  override fun pause(playback: Playback<ViewGroup>) {
    bridge.playerView = null
    bridge.pause()
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

  override fun bind(
    target: ViewGroup,
    config: Playback.Config,
    cb: ((Playback<ViewGroup>) -> Unit)?
  ) {
    val manager = kohii.findSuitableManager(target) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )
    Log.w("Kohii::X", "bind: $target, $manager")
    val result = manager.performBindPlayable(this, target, config,
        object : PlaybackCreator<ViewGroup> {
          override fun createPlayback(
            target: ViewGroup,
            config: Config
          ): Playback<ViewGroup> {
            val container = manager.findSuitableContainer(target)
                ?: throw IllegalStateException(
                    "This manager $this has no Container that " +
                        "accepts this target: $target. Kohii requires at least one."
                )
            return ViewGroupPlayback(
                kohii, this@ViewGroupPlayable, manager, container, target, config,
                manager.parent.playerViewPool
            )
          }
        })
    cb?.invoke(result)
  }
}