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

class Binder<PLAYER>(
  private val kohii: Kohii,
  private val playableCreator: PlayableCreator<PLAYER>,
  private val media: Media
) {

  var playableConfig = Playable.Config()

  inline fun config(config: () -> Playable.Config): Binder<PLAYER> {
    this.playableConfig = config.invoke()
        .copySelf()
    return this
  }

  fun <TARGET : Any> bind(
    target: TARGET,
    config: Playback.Config = Playback.Config(), // default
    cb: ((Playback<TARGET, PLAYER>) -> Unit)? = null
  ): Rebinder {
    val tag = playableConfig.tag
    val toCreate: Playable<PLAYER> by lazy {
      this.playableCreator.createPlayable(
          kohii, media, playableConfig
      )
    }

    val playable = if (tag != null) {
      val cache = kohii.mapTagToPlayable[tag]
      if (cache?.second !== playableCreator.playerType) { // cached Playable of different type will be replaced.
        toCreate.also {
          kohii.mapTagToPlayable[tag] = Pair(it, playableCreator.playerType)
          cache?.first?.release()
        }
      } else {
        @Suppress("UNCHECKED_CAST")
        cache.first as Playable<PLAYER>
      }
    } else {
      toCreate
    }

    kohii.mapPlayableToManager[playable] = null
    playable.bind(target, config, cb)
    return Rebinder(tag, playableCreator.playerType)
  }
}
