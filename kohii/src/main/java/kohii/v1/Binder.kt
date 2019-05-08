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

import com.google.android.exoplayer2.PlaybackParameters
import kohii.media.Media
import kohii.v1.Playable.RepeatMode

class Binder<OUTPUT : Any>(
  private val kohii: Kohii,
  private val media: Media,
  private val playableCreator: PlayableCreator<OUTPUT>
) {

  data class Config(
    var tag: String? = null,
    var prefetch: Boolean = false,
    @RepeatMode var repeatMode: Int = Playable.REPEAT_MODE_OFF,
    var parameters: PlaybackParameters = PlaybackParameters.DEFAULT
  ) {

    internal fun createPlayableConfig(): Playable.Config {
      return Playable.Config(this.tag, this.prefetch, this.repeatMode, this.parameters)
    }
  }

  internal val config = Config()

  fun with(config: Config.() -> Unit): Binder<OUTPUT> {
    this.config.apply(config)
    return this
  }

  fun <CONTAINER : Any> bind(
    target: Target<CONTAINER, OUTPUT>,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = this.config.tag
    val playable = requestPlayable(tag)
    playable.bind(target, config, cb)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  fun <CONTAINER : Any> bind(
    target: CONTAINER,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = this.config.tag
    val playable = requestPlayable(tag)
    playable.bind(target, config, cb)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  private fun requestPlayable(tag: Any?): Playable<OUTPUT> {
    val toCreate: Playable<OUTPUT> by lazy {
      this.playableCreator.createPlayable(kohii, media, this.config.createPlayableConfig())
    }

    val playable =
      if (tag != null) {
        val cache = kohii.mapTagToPlayable[tag]
        if (cache?.second !== playableCreator.outputHolderType) {
          // cached Playable of different output type will be replaced.
          toCreate.also {
            kohii.mapTagToPlayable[tag] = Pair(it, playableCreator.outputHolderType)
            cache?.first?.release()
          }
        } else {
          @Suppress("UNCHECKED_CAST")
          cache.first as Playable<OUTPUT>
        }
      } else {
        toCreate
      }

    kohii.mapPlayableToManager[playable] = null
    return playable
  }
}
