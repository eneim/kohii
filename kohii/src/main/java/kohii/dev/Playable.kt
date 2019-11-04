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

package kohii.dev

import kohii.dev.Playback.Callback
import kohii.logDebug
import kohii.logInfo
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.v1.Bridge
import kotlin.properties.Delegates

open class Playable<RENDERER : Any>(
  val master: Master,
  val media: Media,
  val config: Config,
  val rendererType: Class<RENDERER>,
  val bridge: Bridge<RENDERER>
) : Callback {

  data class Config(
    val tag: Any? = null
  )

  private var playRequested: Boolean = false

  val tag: Any
    get() = config.tag ?: Master.NO_TAG

  open fun onPlay() {
    if (!playRequested) {
      playRequested = true
      bridge.ensurePreparation()
      bridge.play()
      "Play ${this.tag}".logDebug("Kohii::Dev")
    }
  }

  open fun onPause() {
    if (playRequested) {
      playRequested = false
      bridge.pause()
      "Pause ${this.tag}".logDebug("Kohii::Dev")
    }
  }

  open fun onRelease() {
    "Release ${this.tag}".logDebug("Kohii::Dev")
  }

  internal var manager: Manager? by Delegates.observable<Manager?>(
      null,
      onChange = { _, prev, next ->
        if (prev === next) return@observable
        // "$this -- Manager: $prev --> $next".logInfo("Kohii::Dev")
      }
  )

  internal var playback: Playback<*>? by Delegates.observable<Playback<*>?>(null,
      onChange = { _, prev, next ->
        if (prev === next) return@observable
        prev?.callbacks?.remove(this)
        this.manager = next?.manager
        next?.callbacks?.add(this)
      }
  )

  internal var playbackInfo: PlaybackInfo
    get() = bridge.playbackInfo
    set(value) {
      bridge.playbackInfo = value
    }

  // Playback.Callback

  override fun onActive(playback: Playback<*>) {
    check(playback === this.playback)
    "Active ${this.tag} -- $playback".logInfo("Kohii::Dev")
    bridge.prepare(false) // TODO when should prepare?
    bridge.playerView = manager?.acquireRenderer(playback, this)
    master.tryRestorePlaybackInfo(this)
  }

  override fun onInActive(playback: Playback<*>) {
    check(playback === this.playback)
    "InActive ${this.tag} -- $playback".logInfo("Kohii::Dev")
    master.trySavePlaybackInfo(this)
    manager?.releaseRenderer(playback, this)
    bridge.playerView = null
  }

  override fun onAdded(playback: Playback<*>) {
    "Add ${this.tag} -- $playback".logInfo("Kohii::Dev")
  }

  override fun onRemoved(playback: Playback<*>) {
    "Remove ${this.tag} -- $playback".logInfo("Kohii::Dev")
    if (this.playback === playback) this.playback = null
    if (this.manager == null) master.tearDown(this)
  }
}
