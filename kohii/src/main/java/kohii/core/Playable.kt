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

package kohii.core

import kohii.core.Master.MemoryMode
import kohii.core.Master.MemoryMode.AUTO
import kohii.core.Master.MemoryMode.BALANCED
import kohii.core.Master.MemoryMode.HIGH
import kohii.core.Master.MemoryMode.INFINITE
import kohii.core.Master.MemoryMode.LOW
import kohii.core.Master.MemoryMode.NORMAL
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.v1.Bridge
import kotlin.properties.Delegates

open class Playable<RENDERER : Any>(
  val master: Master,
  val media: Media,
  val config: Config,
  internal val rendererType: Class<RENDERER>,
  internal val bridge: Bridge<RENDERER>
) : Playback.Callback, Playback.OnDistanceChangedListener {

  data class Config(
    val tag: Any? = null
  )

  private var playRequested: Boolean = false

  val tag: Any = config.tag ?: Master.NO_TAG

  // Ensure the preparation for the playback
  internal fun onReady() {
    bridge.ensurePreparation()
  }

  internal fun onPlay() {
    if (!playRequested) {
      playRequested = true
      playback?.onPlay()
      bridge.play()
    }
  }

  internal fun onPause() {
    if (playRequested) {
      playRequested = false
      bridge.pause()
      playback?.onPause()
    }
  }

  open fun onRelease() {
    master.trySavePlaybackInfo(this)
    master.releasePlayable(this)
  }

  private val memoryMode: MemoryMode
    get() = (manager as? Manager)?.memoryMode ?: LOW

  internal var manager: PlayableManager? by Delegates.observable<PlayableManager?>(
      null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        if (to == null) master.tearDown(this, false)
      }
  )

  @Suppress("IfThenToElvis")
  internal var playback: Playback<*>? by Delegates.observable<Playback<*>?>(null,
      onChange = { _, from, to ->
        if (from === to) return@observable
        from?.let {
          bridge.removeErrorListener(it)
          bridge.removeEventListener(it)
          it.removeCallback(this)
          if (it.onDistanceChangedListener === this) it.onDistanceChangedListener = null
        }

        this.manager =
          if (to != null) {
            to.manager
          } else {
            val configChange =
              if (from != null) from.manager.group.activity.isChangingConfigurations else false
            if (configChange) master else null
          }

        to?.let {
          it.addCallback(this)
          it.config.callbacks.forEach { cb -> it.addCallback(cb) }
          bridge.addEventListener(it)
          bridge.addErrorListener(it)
          it.onDistanceChangedListener = this
        }
      }
  )

  internal var playbackInfo: PlaybackInfo
    get() = bridge.playbackInfo
    set(value) {
      bridge.playbackInfo = value
    }

  internal val playerState: Int
    get() = bridge.playbackState

  // Playback.Callback

  override fun onActive(playback: Playback<*>) {
    require(playback === this.playback)
    master.tryRestorePlaybackInfo(this)
    master.preparePlayable(this, playback.config.preload)
    val renderer = (manager as? Manager)?.acquireRenderer(playback, this)
    bridge.playerView = renderer
  }

  override fun onInActive(playback: Playback<*>) {
    require(playback === this.playback)
    (manager as? Manager)?.releaseRenderer(playback, this)
    bridge.playerView = null
    if (!playback.manager.group.activity.isChangingConfigurations) {
      master.trySavePlaybackInfo(this)
      master.releasePlayable(this)
    }
  }

  override fun onAdded(playback: Playback<*>) {
    bridge.repeatMode = playback.config.repeatMode
  }

  override fun onRemoved(playback: Playback<*>) {
    require(playback === this.playback)
    this.playback = null // Will also clear current Manager if need.
  }

  override fun toString(): String {
    return "${super.toString()}::$tag"
  }

  // Playback.OnDistanceChangedListener

  override fun onDistanceChanged(
    from: Int,
    to: Int
  ) {
    if (to == 0) {
      master.tryRestorePlaybackInfo(this)
      master.preparePlayable(this, playback?.config?.preload ?: false)
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
        if (memoryMode != BALANCED) {
          bridge.reset(false)
        }
      }
    }
  }
}
