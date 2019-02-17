/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.motion.MotionFragment
import kohii.v1.sample.ui.player.InitData
import kohii.v1.sample.ui.player.PlayerActivity
import kohii.v1.sample.ui.rview.RecyclerViewFragment
import kohii.v1.sample.ui.sview.ScrollViewFragment
import kotlinx.android.synthetic.main.fragment_debug.bindSameView
import kotlinx.android.synthetic.main.fragment_debug.openRecyclerView
import kotlinx.android.synthetic.main.fragment_debug.openScrollView1
import kotlinx.android.synthetic.main.fragment_debug.openScrollView2
import kotlinx.android.synthetic.main.fragment_debug.playerContainer
import kotlinx.android.synthetic.main.fragment_debug.playerView
import kotlinx.android.synthetic.main.fragment_debug.playerView2
import kotlinx.android.synthetic.main.fragment_debug.scrollView
import kotlinx.android.synthetic.main.fragment_debug.switchView
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author eneim (2018/07/13).
 */
@Suppress("unused")
class DebugFragment : BaseFragment(), ContainerProvider {

  companion object {
    fun newInstance() = DebugFragment()
    // const val videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    const val videoUrl = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_hd.mpd"
  }

  val kohii: Kohii by lazy { Kohii[requireContext()] }

  @Suppress("USELESS_CAST")
  private val playable: Playable<PlayerView> by lazy {
    kohii.setUp(Uri.parse(videoUrl))
        .copy(repeatMode = Player.REPEAT_MODE_ONE)
        .copy(tag = "${javaClass.canonicalName}::$videoUrl")
        .asPlayable()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    playable.bind(this, playerView)

    val views = arrayOf(playerView, playerView2)
    val current = AtomicInteger(0)

    // Click to open activity
    playerContainer.setOnClickListener {
      startActivity(
          PlayerActivity.createIntent(
              requireContext(), InitData(
              tag = "${javaClass.canonicalName}::$videoUrl",
              aspectRatio = 1920 / 1080.toFloat()
          )
          )
      )
    }

    // Debug some certain functions.
    switchView.setOnClickListener {
      playable.bind(this, views[current.incrementAndGet() % views.size])
    }

    bindSameView.setOnClickListener {
      playable.bind(this, views[current.get() % views.size])
    }

    // Open the demo for RecyclerView.
    openRecyclerView.setOnClickListener {
      fragmentManager!!.beginTransaction()
          .replace(
              R.id.fragmentContainer, RecyclerViewFragment.newInstance(),
              RecyclerViewFragment::class.java.canonicalName
          )
          .addToBackStack(null)
          .commit()
    }

    // Open the demo for simple ScrollView.
    openScrollView1.setOnClickListener {
      fragmentManager!!.beginTransaction()
          .replace(
              R.id.fragmentContainer, ScrollViewFragment.newInstance(),
              ScrollViewFragment::class.java.canonicalName
          )
          .addToBackStack(null)
          .commit()
    }

    // Open the demo for a more complicated ScrollView.
    openScrollView2.setOnClickListener {
      fragmentManager!!.beginTransaction()
          .replace(
              R.id.fragmentContainer, MotionFragment.newInstance(),
              MotionFragment::class.java.canonicalName
          )
          .addToBackStack(null)
          .commit()
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  }

  override fun provideContainers(): Array<Any>? {
    return arrayOf(scrollView)
  }

  override fun provideContext(): Context {
    return requireContext()
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return viewLifecycleOwner
  }
}