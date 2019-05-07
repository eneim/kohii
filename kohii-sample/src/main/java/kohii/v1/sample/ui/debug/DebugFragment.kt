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
import androidx.core.view.doOnLayout
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.PlaybackEventListener
import kohii.v1.Scope
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_debug.muteSwitch
import kotlinx.android.synthetic.main.fragment_debug.playerView1
import kotlinx.android.synthetic.main.fragment_debug.playerView2
import kotlinx.android.synthetic.main.fragment_debug.scopes
import kotlinx.android.synthetic.main.fragment_debug.scrollView

class DebugFragment : BaseFragment() {

  companion object {
    fun newInstance() = DebugFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  lateinit var playback: Playback<*>

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this]
    val manager = kohii.register(this, scrollView)

    kohii.setUp(videoUrl)
        .config { Playable.Config(tag = "$videoUrl::1") }
        .bind(playerView1) {
          it.addPlaybackEventListener(object : PlaybackEventListener {
            override fun onFirstFrameRendered(playback: Playback<*>) {
              super.onFirstFrameRendered(playback)
              this@DebugFragment.playback = playback
            }
          })
        }

    kohii.setUp(videoUrl)
        .config { Playable.Config(tag = "$videoUrl::2") }
        .bind(playerView2) {
          it.addPlaybackEventListener(object : PlaybackEventListener {
            override fun onFirstFrameRendered(playback: Playback<*>) {
              super.onFirstFrameRendered(playback)
              this@DebugFragment.playback = playback
            }
          })
        }

    val scopeMap = mapOf(
        R.id.scopePlayback to Scope.PLAYBACK,
        R.id.scopeHost to Scope.HOST,
        R.id.scopeManager to Scope.MANAGER
    )

    var currentScope: Scope = scopeMap[scopes.checkedRadioButtonId] ?: Scope.PLAYBACK.also {
      scopes.check(R.id.scopePlayback)
    }

    val unmuteVolume = VolumeInfo()
    val muteVolume = VolumeInfo(true)

    scopes.setOnCheckedChangeListener { _, checkedId ->
      currentScope = scopeMap[checkedId] ?: error("No scope found for $checkedId")
    }

    view.doOnLayout {
      muteSwitch.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          manager.applyVolumeInfo(muteVolume, playback, currentScope)
        } else {
          manager.applyVolumeInfo(unmuteVolume, playback, currentScope)
        }
      }
    }
  }
}
