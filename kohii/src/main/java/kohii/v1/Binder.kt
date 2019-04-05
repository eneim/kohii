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

import kohii.media.Media

class Binder<OUTPUT>(
  private val kohii: Kohii,
  private val playableCreator: PlayableCreator<OUTPUT>,
  private val media: Media
) {

  var playableConfig = Playable.Config()

  inline fun config(config: () -> Playable.Config): Binder<OUTPUT> {
    this.playableConfig = config.invoke()
        .copySelf()
    return this
  }

  fun <CONTAINER : Any> bind(
    target: CONTAINER,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = playableConfig.tag
    val toCreate: Playable<OUTPUT> by lazy {
      this.playableCreator.createPlayable(
          kohii, media, playableConfig
      )
    }

    val playable = if (tag != null) {
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
    playable.bind(target, config, cb)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }
}
