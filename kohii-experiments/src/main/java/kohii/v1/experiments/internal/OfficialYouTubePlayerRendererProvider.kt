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

import androidx.fragment.app.FragmentManager
import kohii.v1.core.Playback
import kohii.v1.core.RendererProvider
import kohii.v1.experiments.YouTubePlayerFragment
import kohii.v1.media.Media

internal class OfficialYouTubePlayerRendererProvider(
  private val fragmentManager: FragmentManager
) : RendererProvider {

  override fun acquireRenderer(
    playback: Playback,
    media: Media
  ): Any {
    val fragment = fragmentManager.findFragmentByTag(playback.tag.toString())
    return fragment ?: YouTubePlayerFragment.newInstance()
  }
}
