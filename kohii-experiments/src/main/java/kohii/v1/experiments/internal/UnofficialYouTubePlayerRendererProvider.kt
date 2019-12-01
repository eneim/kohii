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

package kohii.v1.experiments.internal

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.core.Playback
import kohii.v1.core.RecycledRendererProvider

internal class UnofficialYouTubePlayerRendererProvider(
  poolSize: Int
) : RecycledRendererProvider(poolSize) {

  constructor() : this(2)

  override fun createRenderer(
    playback: Playback,
    rendererType: Int
  ): Any {
    val iFramePlayerOptions = IFramePlayerOptions.Builder()
        .controls(0)
        .build()

    val container = playback.container
    return YouTubePlayerView(container.context).also {
      it.enableAutomaticInitialization = false
      it.enableBackgroundPlayback(false)
      it.getPlayerUiController()
          .showUi(false)
      it.initialize(object : AbstractYouTubePlayerListener() {}, true, iFramePlayerOptions)
    }
  }

  override fun onClear(renderer: Any) {
    (renderer as? YouTubePlayerView)?.release()
  }
}
