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

package kohii.v1.sample.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import kohii.v1.Kohii
import kohii.v1.Playback
import kohii.v1.Playback.Callback
import kohii.v1.PlayerEventListener
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.fragment_player.playerView

class PlayerDialogFragment : AppCompatDialogFragment(), Callback {

  override fun onActive(
    playback: Playback<*>,
    target: Any?
  ) {
    (parentFragment as? Callback)?.onDialogActive(playback.tag)
  }

  override fun onInActive(
    playback: Playback<*>,
    target: Any?
  ) {
    (parentFragment as? Callback)?.onDialogInActive(playback.tag)
  }

  companion object {
    private const val KEY_PLAYABLE_TAG = "kohii:player:dialog:tag"

    fun newInstance(tag: String): PlayerDialogFragment {
      val bundle = Bundle().also {
        it.putString(KEY_PLAYABLE_TAG, tag)
      }
      return PlayerDialogFragment().also { it.arguments = bundle }
    }
  }

  // Interface to tell ParentFragment about status of this Dialog.
  interface Callback {

    fun onDialogActive(tag: Any)

    fun onDialogInActive(tag: Any)
  }

  var listener: PlayerEventListener? = null
  var playback: Playback<*>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    listener = object : PlayerEventListener {
      override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
      ) {
        startPostponedEnterTransition()
        playback?.removePlayerEventListener(this)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_player, container, false)
  }

  override fun onStart() {
    super.onStart()
    val playableTag = arguments?.getString(KEY_PLAYABLE_TAG) as String
    // Only here dialog's window will finally have the DecorView.
    playback = Kohii[dialog.window!!].findPlayable(playableTag)
        ?.bind(playerView)
        ?.also {
          it.addPlayerEventListener(listener!!)
          it.addCallback(this@PlayerDialogFragment)
        }
  }

  override fun onStop() {
    super.onStop()
    playback?.also {
      it.removePlayerEventListener(listener)
      it.removeCallback(this@PlayerDialogFragment)
    }
  }
}