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

import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import kohii.core.Master.Companion.NO_TAG
import kohii.core.Playable.Config
import kohii.media.Media

class Binder<RENDERER : Any>(
  private val engine: Engine<RENDERER>,
  val media: Media
) {

  class Options {
    var tag: Any = NO_TAG
    var threshold: Float = 0.65F
    var delay: Int = 0
    var preload: Boolean = false
    var repeatMode: Int = Common.REPEAT_MODE_OFF
    var controller: Playback.Controller? = null
    var callbacks: Array<Playback.Callback> = emptyArray()
  }

  @RestrictTo(LIBRARY)
  val options = Options()

  inline fun with(options: Options.() -> Unit): Binder<RENDERER> {
    this.options.apply(options)
    return this
  }

  fun bind(
    container: ViewGroup,
    callback: ((Playback) -> Unit)? = null
  ): Rebinder? {
    val tag = options.tag
    val playable = providePlayable(media, tag, Config(tag = tag))
    engine.master.bind(playable, tag, container, options, callback)
    return if (tag != NO_TAG) Rebinder(tag) else null
  }

  private fun providePlayable(
    media: Media,
    tag: Any,
    config: Config
  ): Playable<RENDERER> {
    var cache = engine.master.playables.asSequence()
        .filterNot { it.value == NO_TAG } // only care about tagged Playables
        .filter { it.value == tag /* equals */ }
        .firstOrNull()
        ?.key

    if (cache != null) {
      require(cache.media == media) // Playable of same tag must have the same Media data.
      if (cache.config != config ||
          !engine.creator.rendererType.isAssignableFrom(cache.rendererType)
      ) {
        // Scenario: client bind a Video of same tag/media but different Renderer type or Config.
        cache.playback = null // will also set Manager to null
        engine.master.tearDown(cache, true)
        cache = null
      }
    }

    @Suppress("UNCHECKED_CAST")
    return cache as? Playable<RENDERER> ?: engine.creator.createPlayable(
        engine.master, config, media
    )
  }
}
