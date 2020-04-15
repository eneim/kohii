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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.experiments.internal.OfficialYouTubePlayerPlayableCreator
import kohii.v1.experiments.internal.OfficialYouTubePlayerRendererProvider
import kohii.v1.utils.Capsule

class OfficialYouTubePlayerEngine private constructor(
  master: Master
) : Engine<YouTubePlayerFragment>(
    master,
    OfficialYouTubePlayerPlayableCreator(master)
), DefaultLifecycleObserver {

  private constructor(context: Context) : this(Master[context])

  companion object : Capsule<OfficialYouTubePlayerEngine, Context>(
      ::OfficialYouTubePlayerEngine
  ) {

    @JvmStatic // convenient static call for Java
    operator fun get(context: Context) = super.getInstance(context)

    @JvmStatic // convenient static call for Java
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  override fun prepare(manager: Manager) {
    val fragmentManager =
      when (val host = manager.host) {
        is FragmentActivity -> host.supportFragmentManager
        is Fragment -> host.childFragmentManager
        else -> throw IllegalArgumentException("${host.javaClass} is not supported.")
      }

    val provider = OfficialYouTubePlayerRendererProvider(fragmentManager)
    manager.registerRendererProvider(YouTubePlayerFragment::class.java, provider)
  }
}
