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

package kohii.v1.sample.ui.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import kohii.v1.Kohii
import kohii.v1.LifecycleOwnerProvider
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.PlaybackManager
import kohii.v1.exo.DefaultControlDispatcher
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_scroll_view.playerView
import kotlinx.android.synthetic.main.fragment_scroll_view.scrollView

class NoOpsContainerFragment : BaseFragment(), LifecycleOwnerProvider {

  companion object {
    fun newInstance() = NoOpsContainerFragment()

    // Big Buck Bunny
    const val videoUrl = "https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8"
  }

  private val videoTag by lazy { "${javaClass.canonicalName}::$videoUrl" }
  private lateinit var kohii: Kohii
  private lateinit var manager: PlaybackManager
  private lateinit var playback: Playback<*>

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_scroll_view, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this].also { manager = it.register(this, arrayOf(scrollView)) }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    kohii.setUp(videoUrl)
        .config { Playable.Config(tag = videoTag, repeatMode = Playable.REPEAT_MODE_ONE) }
        .bind(
            playerView,
            config = Playback.Config(controller = DefaultControlDispatcher(manager, playerView))
        ) {
          playback = it
        }
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}
