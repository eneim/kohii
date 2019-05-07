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

import kohii.Beta
import kohii.media.Media

// TODO better Config overriding mechanism.
class Binder<OUTPUT : Any>(
  private val kohii: Kohii,
  private val media: Media,
  private val playableCreator: PlayableCreator<OUTPUT>
) {

  @Beta
  data class Config(
    var tag: Any? = null
  ) {

    operator fun invoke(config: Config) {
      config.tag = this.tag
    }
  }

  var playableConfig = Playable.Config()

  inline fun config(config: () -> Playable.Config): Binder<OUTPUT> {
    this.playableConfig = config.invoke()
        .copySelf()
    return this
  }

  @Beta
  val config = Config()

  @Beta
  inline fun configs(handle: Config.() -> Unit): Binder<OUTPUT> {
    handle.invoke(this.config)
    return this
  }

  fun <CONTAINER : Any> bind(
    target: Target<CONTAINER, OUTPUT>,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = playableConfig.tag
    val playable = requestPlayable(tag)
    playable.bind(target, config, cb)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  fun <CONTAINER : Any> bind(
    target: CONTAINER,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = playableConfig.tag
    val playable = requestPlayable(tag)
    playable.bind(target, config, cb)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  private fun requestPlayable(tag: Any?): Playable<OUTPUT> {
    val toCreate: Playable<OUTPUT> by lazy {
      this.playableCreator.createPlayable(kohii, media, playableConfig)
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
