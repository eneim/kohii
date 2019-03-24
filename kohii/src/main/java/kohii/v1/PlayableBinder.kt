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

import android.net.Uri
import kohii.media.Media
import kohii.media.MediaItem
import kohii.v1.Playback.Config

abstract class PlayableBinder<PLAYER>(
  protected val kohii: Kohii,
  @Suppress("MemberVisibilityCanBePrivate")
  protected val playerType: Class<PLAYER>
) {

  var media: Media? = null
  var playableConfig = Playable.Config()

  fun setUp(uri: Uri) = this.setUp(MediaItem(uri))

  fun setUp(url: String) = this.setUp(Uri.parse(url))

  fun setUp(media: Media): PlayableBinder<PLAYER> {
    this.media = media
    return this
  }

  inline fun config(config: () -> Playable.Config): PlayableBinder<PLAYER> {
    this.playableConfig = config.invoke()
        .copySelf()
    return this
  }

  fun <TARGET : Any> bind(
    target: TARGET,
    config: Config = Playback.Config(), // default
    cb: ((Playback<TARGET, PLAYER>) -> Unit)? = null
  ): Rebinder {
    val media = this.media ?: throw IllegalStateException("Media is not set.")
    val tag = playableConfig.tag
    val toCreate: Playable<PLAYER> by lazy { this.createPlayable(kohii, media, playableConfig) }
    val playable = if (tag != null) {
      val cache = kohii.mapTagToPlayable[tag]
      // cached Playable of different type will be replaced.
      @Suppress("UNCHECKED_CAST")
      if (cache?.second !== playerType) toCreate.also {
        kohii.mapTagToPlayable[tag] = Pair(it, playerType)
        cache?.first?.release()
      } else cache.first as Playable<PLAYER>
    } else {
      toCreate
    }
    kohii.mapPlayableToManager[playable] = null
    playable.bind(target, config, cb)
    return Rebinder(tag, playerType)
  }

  abstract fun createPlayable(
    kohii: Kohii,
    media: Media,
    config: Playable.Config
  ): Playable<PLAYER>
}
