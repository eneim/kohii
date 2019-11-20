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

package kohii.v1.exoplayer

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ExoPlayer
import kohii.v1.core.Engine
import kohii.v1.core.Group
import kohii.v1.core.PlayableCreator
import kohii.v1.exoplayer.internal.PlayerViewPlayableCreator
import kohii.v1.exoplayer.internal.PlayerViewProvider

@ExoPlayer
class Kohii private constructor(
  context: Context,
  playableCreator: PlayableCreator<PlayerView>
) : Engine<PlayerView>(context, playableCreator) {

  companion object {

    @Volatile private var kohii: Kohii? = null

    @JvmStatic
    operator fun get(context: Context): Kohii = kohii ?: synchronized(Kohii::javaClass) {
      kohii ?: Kohii(context, PlayerViewPlayableCreator(context))
          .also { kohii = it }
    }

    @JvmStatic
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  override fun inject(group: Group) {
    group.registerRendererProvider(PlayerView::class.java, PlayerViewProvider())
  }
}
