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
import com.google.android.exoplayer2.Player
import kohii.core.Playable.Config
import kohii.media.Media

class Binder<RENDERER : Any>(
  private val master: Master,
  val media: Media,
  private val playableCreator: PlayableCreator<RENDERER>
) {

  class Options {
    var tag: Any? = null
    var delay: Int = 0
    var preload: Boolean = false
    var repeatMode: Int = Player.REPEAT_MODE_OFF
    var controller: Playback.Controller? = null
    var callbacks: Array<Playback.Callback> = emptyArray()
  }

  @RestrictTo(LIBRARY)
  val options = Options()

  inline fun with(options: Options.() -> Unit): Binder<RENDERER> {
    this.options.apply(options)
    return this
  }

  fun <CONTAINER : ViewGroup> bind(
    container: CONTAINER,
    callback: ((Playback<*>) -> Unit)? = null
  ): Rebinder<RENDERER>? {
    val tag = options.tag ?: Master.NO_TAG
    val playable = providePlayable(media, tag, Config(tag = options.tag))
    master.bind(playable, tag, container, options, callback)
    return if (tag != Master.NO_TAG) playableCreator.createRebinder(tag) else null
  }

  private fun providePlayable(
    media: Media,
    tag: Any,
    config: Config
  ): Playable<RENDERER> {
    var cache = master.playables.asSequence()
        .filterNot { it.value == Master.NO_TAG } // only care about tagged Playables
        .filter { it.value == tag }
        .firstOrNull()
        ?.key

    if (cache != null) {
      require(cache.media == media) // Playable of same tag must have the same Media data.
      if (cache.config != config ||
          !playableCreator.rendererType.isAssignableFrom(cache.rendererType)
      ) {
        // Scenario: client bind a Video of same tag/media but different Renderer type or Config.
        cache.playback = null // will also set Manager to null
        master.tearDown(cache, true)
        cache = null
      }
    }

    @Suppress("UNCHECKED_CAST")
    return cache as? Playable<RENDERER> ?: playableCreator.createPlayable(master, config, media)
  }
}
