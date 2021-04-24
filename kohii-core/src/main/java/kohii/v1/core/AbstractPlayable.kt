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

package kohii.v1.core

import kohii.v1.core.MemoryMode.AUTO
import kohii.v1.core.MemoryMode.BALANCED
import kohii.v1.core.MemoryMode.HIGH
import kohii.v1.core.MemoryMode.INFINITE
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.core.MemoryMode.NORMAL
import kohii.v1.debugOnly
import kohii.v1.internal.PlayerParametersChangeListener
import kohii.v1.logInfo
import kohii.v1.logWarn
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo

abstract class AbstractPlayable<RENDERER : Any>(
  protected val master: Master,
  media: Media,
  config: Config,
  protected val bridge: Bridge<RENDERER>
) : Playable(media, config), Playback.Callback, PlayerParametersChangeListener {

  override val tag: Any = config.tag

  private var playRequested: Boolean = false

  override fun toString(): String {
    return "Playable([t=$tag][b=$bridge][h=${hashCode()}])"
  }

  // Ensure the preparation for the playback
  override fun onReady() {
    "Playable#onReady $this".logInfo()
    bridge.ready()
  }

  override fun onReset() {
    "Playable#onReset $this".logInfo()
    bridge.reset(true)
  }

  override fun onConfigChange(): Boolean {
    "Playable#onConfigChange $this".logInfo()
    return true
  }

  override fun onPrepare(loadSource: Boolean) {
    "Playable#onPrepare $loadSource $this".logInfo()
    bridge.prepare(loadSource)
  }

  override fun onPlay() {
    "Playable#onPlay $this".logWarn()
    playback?.onPlay()
    if (!playRequested || !bridge.isPlaying()) {
      playRequested = true
      bridge.play()
    }
  }

  override fun onPause() {
    "Playable#onPause $this".logWarn()
    if (playRequested || bridge.isPlaying()) {
      playRequested = false
      bridge.pause()
    }
    playback?.onPause()
  }

  override fun onRelease() {
    "Playable#onRelease $this".logInfo()
    bridge.release()
  }

  override fun isPlaying(): Boolean {
    "Playable#isPlaying $this".logInfo()
    return bridge.isPlaying()
  }

  private val memoryMode: MemoryMode
    get() = (manager as? Manager)?.memoryMode ?: LOW

  override var manager: PlayableManager? = null
    set(value) {
      val oldManager = field
      field = value
      val newManager = field
      if (oldManager === newManager) return
      "Playable#manager $oldManager --> $newManager, $this".logInfo()
      oldManager?.removePlayable(this)
      newManager?.addPlayable(this)
      // Setting Manager to null.
      if (newManager == null) {
        master.trySavePlaybackInfo(this)
        master.tearDown(playable = this)
      } else if (oldManager === null) {
        master.tryRestorePlaybackInfo(this)
      }
    }

  override var playback: Playback? = null
    set(value) {
      val oldPlayback = field
      field = value
      val newPlayback = field
      if (oldPlayback === newPlayback) return
      "Playable#playback $oldPlayback --> $newPlayback, $this".logInfo()
      oldPlayback?.let(::detachFromPlayback)

      this.manager = if (newPlayback != null) {
        newPlayback.manager
      } else {
        val changingConfiguration = oldPlayback?.manager?.isChangingConfigurations() == true
        if (changingConfiguration) {
          if (!onConfigChange()) {
            // On config change, if the Playable doesn't support, we need to pause the Video.
            onPause()
            null
          } else {
            master // to prevent the Playable from being destroyed when Manager is null.
          }
        } else {
          // TODO(eneim): rethink this to support off-screen manual playback/kohiiCanPause().
          /* if (master.manuallyStartedPlayable.get() === this && isPlaying()) master
          else null */
          null
        }
      }

      newPlayback?.let(::attachToPlayback)

      master.notifyPlaybackChanged(this, oldPlayback, newPlayback)
    }

  override val playerState: Int
    get() = bridge.playerState

  private fun attachToPlayback(playback: Playback) {
    playback.playable = this
    playback.playerParametersChangeListener = this
    playback.addCallback(this)
    playback.config.callbacks.forEach { callback -> playback.addCallback(callback) }

    bridge.addEventListener(playback)
    bridge.addErrorListener(playback)

    if (playback.tag != Master.NO_TAG) {
      if (playback.config.controller != null) {
        master.plannedManualPlayables.add(playback.tag)
      } else {
        master.plannedManualPlayables.remove(playback.tag)
      }
    }
  }

  private fun detachFromPlayback(playback: Playback) {
    bridge.removeErrorListener(playback)
    bridge.removeEventListener(playback)
    playback.removeCallback(this)
    debugOnly {
      check(playback.playable === this) {
        """
          Old playback of this playable ($this) is 
          bound to a different playable: ${playback.playable}
        """.trimIndent()
      }
    }
    if (playback.playable === this) playback.playable = null
    if (playback.playerParametersChangeListener === this) {
      playback.playerParametersChangeListener = null
    }
  }

  // Playback.Callback

  override fun onActive(playback: Playback) {
    "Playable#onActive $playback, $this".logInfo()
    require(playback === this.playback)
    master.tryRestorePlaybackInfo(this)
    master.preparePlayable(this, playback.config.preload)
    bridge.playerParameters = playback.playerParameters
  }

  override fun onInActive(playback: Playback) {
    "Playable#onInActive $playback, $this".logInfo()
    require(playback === this.playback)
    val configChange = playback.manager.isChangingConfigurations()
    if (!configChange && master.releasePlaybackOnInActive(playback)) {
      master.trySavePlaybackInfo(this)
      master.releasePlayable(this)
    }
  }

  override fun onAdded(playback: Playback) {
    "Playable#onAdded $playback, $this".logInfo()
    bridge.repeatMode = playback.config.repeatMode
    bridge.volumeInfo = playback.volumeInfo
  }

  override fun onRemoved(playback: Playback) {
    "Playable#onRemoved $playback, $this".logInfo()
    require(playback === this.playback)
    this.playback = null // Will also clear current Manager.
  }

  override fun onDetached(playback: Playback) {
    "Playable#onDetached $playback, $this".logInfo()
    require(playback === this.playback)
    val configChange = playback.manager.isChangingConfigurations()
    if (!configChange && !master.releasePlaybackOnInActive(playback)) {
      master.trySavePlaybackInfo(this)
      master.releasePlayable(this)
    }
    master.onPlaybackDetached(playback)
  }

  override fun setupRenderer(playback: Playback) {
    "Playable#setupRenderer $playback, $this".logInfo()
    require(playback === this.playback)
    if (this.renderer == null || manager !== playback.manager) {
      // Only request for Renderer if we do not have one.
      val renderer = playback.acquireRenderer()
      if (playback.attachRenderer(renderer)) {
        this.renderer = renderer
        onRendererAttached(playback, renderer)
      }
    }
  }

  protected open fun onRendererAttached(playback: Playback, renderer: Any?) {
    playback.onRendererAttached(renderer)
  }

  override fun teardownRenderer(playback: Playback) {
    "Playable#teardownRenderer $playback, $this".logInfo()
    require(this.playback == null || this.playback === playback)
    val renderer = this.renderer
    if (renderer != null) {
      // Only release the Renderer if we have one to release.
      if (playback.detachRenderer(renderer)) {
        playback.releaseRenderer(renderer)
        this.renderer = null
        onRendererDetached(playback, renderer)
      }
    }
  }

  protected open fun onRendererDetached(playback: Playback, renderer: Any?) {
    playback.onRendererDetached(renderer)
  }

  override fun onPlaybackPriorityChanged(
    playback: Playback,
    oldPriority: Int,
    newPriority: Int
  ) {
    "Playable#onPlaybackPriorityChanged $playback, $oldPriority --> $newPriority, $this".logInfo()
    if (newPriority == 0) {
      master.tryRestorePlaybackInfo(this)
      master.preparePlayable(this, playback.config.preload)
    } else {
      val memoryMode = master.preferredMemoryMode(this.memoryMode)
      val priorityToRelease = when (memoryMode) {
        AUTO, LOW -> 1
        NORMAL -> 2
        BALANCED -> 2 // Same as 'NORMAL', but will keep the 'relative' Playback alive.
        HIGH -> 8
        INFINITE -> Int.MAX_VALUE
      }
      if (newPriority >= priorityToRelease) {
        master.trySavePlaybackInfo(this)
        master.releasePlayable(this)
      } else {
        master.tryRestorePlaybackInfo(this)
        master.preparePlayable(this, playback.config.preload)
        if (memoryMode < BALANCED) {
          bridge.reset(false)
        }
      }
    }
  }

  override fun onVolumeInfoChanged(
    playback: Playback,
    from: VolumeInfo,
    to: VolumeInfo
  ) {
    "Playable#onVolumeInfoChanged $playback, $from --> $to, $this".logInfo()
    if (from != to) bridge.volumeInfo = to
  }

  override var playbackInfo: PlaybackInfo
    get() = bridge.playbackInfo
    set(value) {
      "Playable#playbackInfo setter $value, $this".logInfo()
      bridge.playbackInfo = value
    }

  override fun onUnbind(playback: Playback) {
    "Playable#onUnbind $playback, $this".logInfo()
    if (this.playback === playback) {
      playback.manager.removePlayback(playback)
    }
  }

  override fun onNetworkTypeChanged(
    from: NetworkType,
    to: NetworkType
  ) {
    "Playable#onNetworkTypeChanged $playback, $this".logInfo()
    playback?.onNetworkTypeChanged(to)
  }

  override fun onPlayerParametersChanged(parameters: PlayerParameters) {
    "Playable#onPlayerParametersChanged $parameters, $this".logInfo()
    bridge.playerParameters = parameters
  }
}
