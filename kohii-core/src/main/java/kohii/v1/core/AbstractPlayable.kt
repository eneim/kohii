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
import kohii.v1.core.Playback.Callback
import kohii.v1.core.Playback.PlayerParametersChangeListener
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
) : Playable(media, config), Callback, PlayerParametersChangeListener {

  override val tag: Any = config.tag

  private var playRequested: Boolean = false

  override fun toString(): String {
    return "Playable([t=$tag][b=$bridge][h=${super.hashCode()}])"
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
    return bridge.isPlaying()
  }

  private val memoryMode: MemoryMode
    get() = (manager as? Manager)?.memoryMode ?: LOW

  override var manager: PlayableManager? = null
    set(value) {
      val from = field
      field = value
      val to = field
      if (from === to) return
      "Playable#manager $from --> $to, $this".logInfo()
      if (to == null) {
        master.trySavePlaybackInfo(this)
        master.tearDown(
            playable = this,
            clearState = if (from is Manager) !from.isChangingConfigurations() else true
        )
      } else if (from === null) {
        master.tryRestorePlaybackInfo(this)
      }
    }

  override var playback: Playback? = null
    set(value) {
      val from = field
      field = value
      val to = field
      if (from === to) return
      "Playable#playback $from --> $to, $this".logInfo()
      if (from != null) {
        bridge.removeErrorListener(from)
        bridge.removeEventListener(from)
        from.removeCallback(this)
        if (from.playable === this) from.playable = null
        if (from.playerParametersChangeListener === this) from.playerParametersChangeListener = null
      }

      this.manager = if (to != null) {
        to.manager
      } else {
        val configChange = from?.manager?.isChangingConfigurations() == true
        if (!configChange) {
          // TODO need a better implementation.
          if (master.manuallyStartedPlayable.get() === this && isPlaying()) master
          else null
        } else if (!onConfigChange()) {
          // On config change, if the Playable doesn't support, we need to pause the Video.
          onPause()
          null
        } else {
          master // to prevent the Playable from being destroyed when Manager is null.
        }
      }

      if (to != null) {
        to.playable = this
        to.playerParametersChangeListener = this
        to.addCallback(this)
        to.config.callbacks.forEach { cb -> to.addCallback(cb) }
        bridge.addEventListener(to)
        bridge.addErrorListener(to)
        if (to.tag != Master.NO_TAG) {
          if (to.config.controller != null) master.plannedManualPlayables.add(to.tag)
          else master.plannedManualPlayables.remove(to.tag)
        }
      }

      master.notifyPlaybackChanged(this, from, to)
    }

  override val playerState: Int
    get() = bridge.playerState

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
    if (!configChange && !master.onPlaybackInActive(this, playback)) {
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
    master.onPlaybackDetached(playback)
  }

  override fun considerRequestRenderer(playback: Playback) {
    "Playable#considerRequestRenderer $playback, $this".logInfo()
    require(playback === this.playback)
    if (this.renderer == null || manager !== playback.manager) {
      // Only request for Renderer if we do not have one.
      val renderer = playback.acquireRenderer()
      if (playback.attachRenderer(renderer)) this.renderer = renderer
    }
  }

  override fun considerReleaseRenderer(playback: Playback) {
    "Playable#considerReleaseRenderer $playback, $this".logInfo()
    require(this.playback == null || this.playback === playback)
    val renderer = this.renderer
    if (renderer != null) {
      // Only release the Renderer if we have one to release.
      if (playback.detachRenderer(renderer)) {
        playback.releaseRenderer(renderer)
        this.renderer = null
      }
    }
  }

  override fun onDistanceChanged(
    playback: Playback,
    from: Int,
    to: Int
  ) {
    "Playable#onDistanceChanged $playback, $from --> $to, $this".logInfo()
    if (to == 0) {
      master.tryRestorePlaybackInfo(this)
      master.preparePlayable(this, playback.config.preload)
    } else {
      val memoryMode = master.preferredMemoryMode(this.memoryMode)
      val distanceToRelease =
        when (memoryMode) {
          AUTO, LOW -> 1
          NORMAL -> 2
          BALANCED -> 2 // Same as 'NORMAL', but will keep the 'relative' Playback alive.
          HIGH -> 8
          INFINITE -> Int.MAX_VALUE - 1
        }
      if (to >= distanceToRelease) {
        master.trySavePlaybackInfo(this)
        master.releasePlayable(this)
      } else {
        if (memoryMode !== BALANCED) {
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
