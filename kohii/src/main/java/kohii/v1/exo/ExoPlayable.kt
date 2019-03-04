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
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playable.Builder
import kohii.v1.Playback
import kohii.v1.Playback.Options
import kohii.v1.PlaybackCreator
import kohii.v1.PlayerEventListener
import kohii.v1.ViewPlayback

/**
 * @author eneim (2018/06/24).
 */
class ExoPlayable internal constructor(
  val kohii: Kohii,
  builder: Builder
) : Playable<PlayerView> {

  companion object {
    private const val TAG = "Kohii::PL"
  }

  private val bridge by lazy { kohii.bridgeProvider.provideBridge(builder) }

  private val builderTag = builder.tag
  private val prefetch = builder.prefetch
  private val delay = builder.delay

  private var listener: PlayerEventListener? = null

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
    require(playback.target is PlayerView) {
      "${this.javaClass.simpleName} only works with target of type PlayerView"
    }
    bridge.playerView = playback.target
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
  }

  ////

  override val tag: Any
    get() = builderTag ?: Playable.NO_TAG

  // When binding to a PlayerView, any old Playback for the same PlayerView should be destroyed.
  // Relationship: [Playable] --> [Playback [Target]]
  override fun bind(
    target: PlayerView,
    priority: Int,
    cb: ((Playback<PlayerView>) -> Unit)?
  ) {
    val manager = kohii.findSuitableManager(target) ?: throw IllegalStateException(
        "There is no manager for $target. Forget to register one?"
    )

    Log.w("Kohii::X", "bind: $target, $manager")
    val options = Playback.Options(priority) { delay }
    val result = manager.performBindPlayable(this, target, options,
        object : PlaybackCreator<PlayerView> {
          override fun createPlayback(
            target: PlayerView,
            options: Options
          ): Playback<PlayerView> {
            val container = manager.findSuitableContainer(target)
                ?: throw IllegalStateException(
                    "This manager $this has no Container that " +
                        "accepts this target: $target. Kohii requires at least one."
                )
            return ViewPlayback(kohii, this@ExoPlayable, manager, container, target, options)
          }
        })
    cb?.invoke(result)
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
}