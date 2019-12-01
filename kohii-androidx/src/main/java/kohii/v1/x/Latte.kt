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

package kohii.v1.x

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.media2.widget.VideoView
import kohii.v1.Experiment
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.PlayableCreator
import kohii.v1.utils.SingletonHolder

@Experiment
class Latte private constructor(
  master: Master,
  playableCreator: PlayableCreator<VideoView> = VideoViewPlayableCreator(master)
) : Engine<VideoView>(master, playableCreator) {

  private constructor(context: Context) : this(Master[context])

  companion object : SingletonHolder<Latte, Context>(::Latte) {

    @JvmStatic
    operator fun get(context: Context) = super.getInstance(context)

    @JvmStatic
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  override fun prepare(manager: Manager) {
    manager.registerRendererProvider(VideoView::class.java, VideoViewProvider())
  }
}
