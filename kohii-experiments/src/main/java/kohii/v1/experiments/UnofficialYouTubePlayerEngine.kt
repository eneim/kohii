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

package kohii.v1.experiments

import android.content.Context
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.experiments.internal.UnofficialYouTubePlayerPlayableCreator
import kohii.v1.experiments.internal.UnofficialYouTubePlayerRendererProvider
import kohii.v1.utils.Capsule

class UnofficialYouTubePlayerEngine private constructor(
  master: Master
) : Engine<YouTubePlayerView>(
  master,
  UnofficialYouTubePlayerPlayableCreator(master)
) {

  private constructor(context: Context) : this(Master[context])

  companion object {

    private val capsule =
      Capsule<UnofficialYouTubePlayerEngine, Context>(::UnofficialYouTubePlayerEngine)

    @JvmStatic // convenient static call for Java
    operator fun get(context: Context) = capsule.get(context)

    @JvmStatic // convenient static call for Java
    operator fun get(fragment: Fragment) = capsule.get(fragment.requireContext())
  }

  override fun prepare(manager: Manager) {
    manager.registerRendererProvider(
      YouTubePlayerView::class.java,
      UnofficialYouTubePlayerRendererProvider()
    )
  }
}
