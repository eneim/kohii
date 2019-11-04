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

import com.google.android.exoplayer2.ui.PlayerView
import kohii.dev.Playable.Config
import kohii.media.Media

internal class PlayerViewPlayableProvider(
  val master: Master,
  private val playableCreator: PlayableCreator<PlayerView>
) : PlayableProvider<PlayerView> {

  override fun providePlayable(
    media: Media,
    config: Config
  ): Playable<PlayerView> {
    val tag = config.tag ?: Master.NO_TAG
    var cache = master.playables.asSequence()
        .filter { it.key.media == media }
        .filter { it.value == tag }
        .firstOrNull()
        ?.key

    if (cache != null && (cache.config != config || cache.rendererType !== PlayerView::class.java)) {
      // Scenario: client bind a Video of same tag/media but different Renderer type.
      cache.playback = null
      master.tearDown(cache)
      cache = null
    }

    @Suppress("UNCHECKED_CAST")
    return cache as? Playable<PlayerView> ?: playableCreator.createPlayable(master, config, media)
        .also { master.playables[it] = tag }
  }
}
