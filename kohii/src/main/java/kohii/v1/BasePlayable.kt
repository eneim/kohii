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

import android.view.ViewGroup
import androidx.core.view.doOnAttach
import kohii.logDebug
import kohii.logWarn
import kohii.media.Media
import kohii.media.VolumeInfo
import kohii.v1.Playable.Companion.NO_TAG
import kohii.v1.Playback.Config
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

// RENDERER is what to render the content to. In Kohii, PlayerView is supported out of the box.
abstract class BasePlayable<RENDERER : Any>(
  override val kohii: Kohii,
  override val media: Media,
  override val config: Playable.Config,
  internal val bridge: Bridge<RENDERER>,
  private val playbackCreator: PlaybackCreator<RENDERER>
) : Playable<RENDERER> {

  override fun onAdded(playback: Playback<*>) {
    this.bridge.addEventListener(playback)
    this.bridge.addEventListener(playback.playerListeners)
    this.bridge.addErrorListener(playback.errorListeners)
    this.bridge.addVolumeChangeListener(playback.volumeListeners)

    this.bridge.repeatMode = playback.config.repeatMode
    this.bridge.parameters = playback.config.parameters

    playback.config.playbackInfo?.also { this.playbackInfo = it }
    this.setVolumeInfo(playback.targetHost.volumeInfo)
  }

  override fun onRemoved(playback: Playback<*>) {
    this.bridge.removeVolumeChangeListener(playback.volumeListeners)
    this.bridge.removeEventListener(playback.playerListeners)
    this.bridge.removeErrorListener(playback.errorListeners)
    this.bridge.removeEventListener(playback)

    // Note|eneim|20190113: only call release when there is no more Manager manages this.
    if (this.manager == null) {
      // There is no other Manager to manage this Playable, and we are removing the last one, so ...
      kohii.releasePlayable(config.tag, this)
      kohii.mapPlayableTagToInfo.remove(this.tag)
    }
  }

  override fun onRelease(playback: Playback<*>) {
    if (this.manager == null || !playback.manager.isActive(playback)) {
      this.release()
    }
  }

  override fun <CONTAINER : ViewGroup> bind(
    target: Target<CONTAINER, RENDERER>,
    config: Config,
    onDone: ((Playback<RENDERER>) -> Unit)?
  ) {
    val container = target.container
    val weakOnDone = WeakReference(onDone)
    container.doOnAttach {
      val manager = kohii.findManagerForContainer(container) ?: throw IllegalStateException(
          "There is no manager for $target. Forget to register one?"
      )
      val result = manager.performBindPlayable(
          this, target, config,
          playbackCreator
      )
      weakOnDone.get()
          ?.invoke(result)
    }
  }

  override var manager by Delegates.observable<PlayableManager?>(
      initialValue = null,
      onChange = { _, oldVal, newVal ->
        if (oldVal !== newVal) {
          "$this -- DIFF:MANAGER: from $oldVal to $newVal".logDebug()
          if (newVal != null) kohii.playables.add(this)
          else kohii.playables.remove(this)
        }
      })

  override var playback: Playback<RENDERER>? by Delegates.observable<Playback<RENDERER>?>(
      initialValue = null,
      onChange = { _, prev, next ->
        if (prev === next) return@observable
        "$this -- DIFF:PLAYBACK: from $prev to $next".logWarn()
      }
  )

  override val tag: Any
    get() = config.tag ?: NO_TAG

  override val playbackState: Int
    get() = bridge.playbackState

  override var repeatMode: Int
    get() = this.bridge.repeatMode
    set(value) {
      this.bridge.repeatMode = value
    }

  override fun isPlaying(): Boolean {
    return this.bridge.isPlaying()
  }

  override fun prepare() {
    this.bridge.prepare(config.preLoad)
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

  override fun reset() {
    bridge.reset()
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

  override fun onPlaybackActive(
    playback: Playback<RENDERER>,
    renderer: RENDERER
  ) {
    bridge.playerView = renderer
  }

  override fun onPlaybackInActive(
    playback: Playback<RENDERER>,
    renderer: RENDERER?
  ) {
    if (bridge.playerView === renderer) {
      bridge.playerView = null
    }
  }

  override fun toString(): String {
    val firstPart = "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    val secondPart = "${bridge.javaClass.simpleName}@${Integer.toHexString(bridge.hashCode())}"
    return "$firstPart::$secondPart"
  }
}
