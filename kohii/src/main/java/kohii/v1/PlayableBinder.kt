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

import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.v1.Playback.Config
import kohii.v1.exo.PlayerViewPlayable
import kohii.v1.exo.ViewGroupPlayable

/**
 * @author eneim (2019/03/14)
 *
 * Need to give the Config more power. Because Playable lives at global scope, so is instance of this class.
 * Responsibilities:
 * - Allow binding to specific type of Target.
 * - Judge from the target type to build correct Playable.
 */
class PlayableBinder(
  val kohii: Kohii,
  val media: Media
) {

  var playableConfig = Playable.Config()

  inline fun config(config: () -> Playable.Config): PlayableBinder {
    this.playableConfig = config.invoke()
        .copySelf()
    return this
  }

  fun <T : Any> bind(
    target: T,
    config: Config = Config(), // default
    cb: ((Playback<T>) -> Unit)? = null
  ): Rebinder {
    val tag = playableConfig.tag
    val targetType = target.javaClass
    val toCreate by lazy {
      when {
        PlayerView::class.java.isAssignableFrom(targetType) ->
          PlayerViewPlayable(kohii, media, playableConfig)
        ViewGroup::class.java.isAssignableFrom(targetType) ->
          ViewGroupPlayable(kohii, media, playableConfig)
        else -> throw IllegalArgumentException("Unsupported target type: ${targetType.simpleName}")
      }
    }

    @Suppress("UNCHECKED_CAST")
    val playable = (
        if (tag != null) {
          val cache = kohii.mapTagToPlayable[tag]
          // cached Playable but of different type will be replaced.
          (cache as? Playable<T>) ?: toCreate.also {
            kohii.mapTagToPlayable[tag] = it
            cache?.release()
          }
        } else {
          toCreate
        }) as Playable<T>
    kohii.mapPlayableToManager[playable] = null
    playable.bind(target, config, cb)
    return Rebinder(tag, targetType)
  }
}