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

package kohii.v1.sample.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import kohii.v1.Kohii
import kohii.v1.LifecycleOwnerProvider
import kohii.v1.Playable
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_test_reuse_bridge.playerView
import kotlinx.android.synthetic.main.fragment_test_reuse_bridge.playerViewContainer
import kotlinx.android.synthetic.main.fragment_test_reuse_bridge.scrollView
import kotlinx.android.synthetic.main.fragment_test_reuse_bridge.switchButton
import java.util.concurrent.atomic.AtomicInteger

class ReuseBridgeFragment : BaseFragment(), LifecycleOwnerProvider {

  companion object {
    fun newInstance() = ReuseBridgeFragment()
    const val videoUrl = "https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8"
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_test_reuse_bridge, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    kohii.register(this, arrayOf(scrollView))

    val views = arrayOf(playerView, playerViewContainer)
    val rebinder = kohii.setUp(videoUrl)
        .config { Playable.Config(tag = videoUrl) }
        .bind(playerView)

    val viewCount = views.size
    val current = AtomicInteger(0)
    switchButton.setOnClickListener {
      rebinder.rebind(kohii, views[current.incrementAndGet() % viewCount])
    }
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}
