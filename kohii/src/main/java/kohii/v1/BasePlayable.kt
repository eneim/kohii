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

import android.util.Log
import android.view.ViewGroup
import com.google.android.exoplayer2.ExoPlaybackException
import kohii.media.Media
import kohii.media.VolumeInfo
import kohii.v1.Playable.Companion.NO_TAG
import kohii.v1.Playback.Config

// OUTPUT is what to render the content to. In Kohii, PlayerView is supported out of the box.
abstract class BasePlayable<OUTPUT : Any>(
  protected val kohii: Kohii,
  override val media: Media,
  protected val config: Playable.Config,
  protected val bridge: Bridge<OUTPUT>,
  @Suppress("MemberVisibilityCanBePrivate")
  protected val playbackCreator: PlaybackCreator<OUTPUT>
) : Playable<OUTPUT> {

  private var listener: PlayerEventListener? = null

  override fun onAdded(playback: Playback<*>) {
    if (this.listener == null) {
      this.listener = object : PlayerEventListener {
        override fun onPlayerStateChanged(
          playWhenReady: Boolean,
          playbackState: Int
        ) {
          playback.onPlayerStateChanged(playWhenReady, playbackState)
        }

        override fun onRenderedFirstFrame() {
          playback.onFirstFrameRendered()
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
          Log.e("Kohii::Playable", "Error: ${error?.cause}")
        }
      }.also { this.bridge.addEventListener(it) }
    }
    this.bridge.addErrorListener(playback.errorListeners)
    this.bridge.addEventListener(playback.playerListeners)
    this.bridge.addVolumeChangeListener(playback.volumeListeners)

    playback.config.playbackInfo?.also { this.playbackInfo = it }
    this.setVolumeInfo(playback.targetHost.volumeInfo)
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
      kohii.releasePlayable(config.tag, this)
    }
  }

  // Playback.Callback#onActive(Playback)
  /* Expected:
   * - Instance of this class will have member 'playback' set to the method parameter.
   * - Bridge instance will be set with correct target (PlayerView).
   */
  override fun onActive(playback: Playback<*>) {
    require(playback.target is ViewGroup) {
      "${this.javaClass.name} only works with target of type ViewGroup"
    }
  }

  // Playback.Callback#onInActive(Playback)
  override fun onInActive(playback: Playback<*>) {
    // When a Playback becomes inactive, its Playable may be attached to other Playback already.
    // We only detach the Bridge's PlayerView when the Playable is no longer belong to any Playback,
    // which is equal to: (1) it is not being managed, or (2) it is being managed by the Playback passed
    // to this method, and this Playback is being inactive.
    if (kohii.mapPlayableToManager[this] === playback.manager /* (2) */ ||
        kohii.mapPlayableToManager[this] == null /* (1) */
    ) {
      // This will release current Video MediaCodec instances, which are expensive to retain.
      this.bridge.playerView = null
    }
  }

  protected abstract fun <CONTAINER : Any> createBoxedTarget(target: CONTAINER): Target<CONTAINER, OUTPUT>

  override fun <CONTAINER : Any> bind(
    target: CONTAINER,
    config: Config,
    cb: ((Playback<OUTPUT>) -> Unit)?
  ) {
    val boxedTarget = createBoxedTarget(target)
    this.bind(boxedTarget, config, cb)
  }

  override fun <CONTAINER : Any> bind(
    target: Target<CONTAINER, OUTPUT>,
    config: Config,
    cb: ((Playback<OUTPUT>) -> Unit)?
  ) {
    val manager = kohii.findManagerForContainer(target.requireContainer())
        ?: throw IllegalStateException("There is no manager for $target. Forget to register one?")

    val result = manager.performBindPlayable(
        this, target, config,
        playbackCreator
    )
    cb?.invoke(result)
  }

  override val tag: Any = config.tag ?: NO_TAG

  override var repeatMode: Int
    get() = this.bridge.repeatMode
    set(value) {
      this.bridge.repeatMode = value
    }

  override val isPlaying: Boolean
    get() = this.bridge.isPlaying

  override fun prepare() {
    this.bridge.prepare(config.prefetch)
  }

  override fun ensurePreparation() {
    this.bridge.ensurePreparation()
  }

  override fun play() {
    bridge.play()
  }

  override fun pause() {
    bridge.pause()
  }

  override fun release() {
    this.bridge.release()
  }

  override fun seekTo(positionMs: Long) {
    this.bridge.seekTo(positionMs)
  }

  override var playbackInfo
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
}
