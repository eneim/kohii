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

import android.view.ViewGroup
import kohii.v1.core.Master.Companion.NO_TAG
import kohii.v1.core.Playable.Config
import kohii.v1.core.Playback.ArtworkHintListener
import kohii.v1.core.Playback.Callback
import kohii.v1.core.Playback.Controller
import kohii.v1.core.Playback.NetworkTypeChangeListener
import kohii.v1.core.Playback.TokenUpdateListener
import kohii.v1.media.Media
import kohii.v1.media.PlaybackInfo

class Binder(
  private val engine: Engine<*>,
  internal val media: Media
) {

  /**
   * @property initialPlaybackInfo expected initial [PlaybackInfo] for a [Playable] to start when
   * a new [Playback] is bound to it. If null, it will follow the default behavior: an existing
   * [Playable] will keep its current state, a newly created [Playable] receives a new
   * [PlaybackInfo]. This property is not available in the [Rebinder].
   */
  class Options {
    var tag: Any = NO_TAG
    var threshold: Float = 0.65F
    var delay: Int = 0
    var preload: Boolean = false
    var repeatMode: Int = Common.REPEAT_MODE_OFF
    var controller: Controller? = null
    var initialPlaybackInfo: PlaybackInfo? = null
    var artworkHintListener: ArtworkHintListener? = null
    var tokenUpdateListener: TokenUpdateListener? = null
    var networkTypeChangeListener: NetworkTypeChangeListener? = null
    val callbacks = mutableSetOf<Callback>()
  }

  @JvmSynthetic
  @PublishedApi
  internal val options = Options()

  @JvmOverloads
  fun bind(
    container: ViewGroup,
    callback: ((Playback) -> Unit)? = null
  ): Rebinder? {
    val tag = options.tag
    val playable = providePlayable(
        media, tag,
        Config(tag = tag, rendererType = engine.playableCreator.rendererType)
    )
    engine.master.bind(playable, tag, container, options, callback)
    return if (tag != NO_TAG) Rebinder(tag) else null
  }

  private fun providePlayable(
    media: Media,
    tag: Any,
    config: Config
  ): Playable {
    var cache = engine.master.playables.asSequence()
        .filterNot { it.value == NO_TAG } // only care about tagged Playables
        .filter { it.value == tag /* equals */ }
        .firstOrNull()
        ?.key

    if (cache != null) {
      require(cache.media == media) // Playable of same tag must have the same Media data.
      if (cache.config != config /* equals */) {
        // Scenario: client bind a Video of same tag/media but different Renderer type or Config.
        cache.playback = null // will also set Manager to null
        engine.master.tearDown(cache, true)
        cache = null
      }
    }

    return cache ?: engine.playableCreator.createPlayable(config, media)
  }
}
