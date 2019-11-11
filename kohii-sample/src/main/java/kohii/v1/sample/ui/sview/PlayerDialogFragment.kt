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

package kohii.v1.sample.ui.sview

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Master
import kohii.core.Playback
import kohii.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.ui.player.InitData
import kotlinx.android.synthetic.main.fragment_player.playerContainer
import kotlinx.android.synthetic.main.fragment_player.playerView

class PlayerDialogFragment : AppCompatDialogFragment(), Playback.Callback {

  companion object {
    private const val KEY_INIT_DATA = "kohii::player::init_data"
    private const val KEY_REBINDER = "kohii:player:dialog:rebinder"

    fun newInstance(
      rebinder: Rebinder<PlayerView>,
      initData: InitData
    ): PlayerDialogFragment {
      val bundle = Bundle().also {
        it.putParcelable(KEY_REBINDER, rebinder)
        it.putParcelable(KEY_INIT_DATA, initData)
      }
      return PlayerDialogFragment()
          .also { it.arguments = bundle }
    }
  }

  // Interface to tell ParentFragment about status of this Dialog.
  interface Callback {

    fun onDialogActive()

    fun onDialogInActive(rebinder: Rebinder<PlayerView>)
  }

  private lateinit var kohii: Master
  private lateinit var rebinder: Rebinder<PlayerView>

  private var playback: Playback<*>? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    (dialog as? AppCompatDialog)?.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    return dialog
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_player, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val initData = requireNotNull(requireArguments().getParcelable<InitData>(KEY_INIT_DATA))
    playerContainer.setAspectRatio(initData.aspectRatio)

    kohii = Master[this].also {
      it.register(this)
          .attach(playerContainer)
    }
  }

  override fun onStart() {
    super.onStart()
    val rebinder =
      requireNotNull(requireArguments().getParcelable<Rebinder<PlayerView>>(KEY_REBINDER))
    rebinder.with {
      callbacks = arrayOf(this@PlayerDialogFragment)
    }
        .bind(kohii, playerView) { playback = it }
    this.rebinder = rebinder
  }

  override fun onActive(playback: Playback<*>) {
    (parentFragment as? Callback)?.onDialogActive()
  }

  // Would be called after onStop()
  override fun onInActive(playback: Playback<*>) {
    playback.container.post {
      (parentFragment as? Callback)?.onDialogInActive(rebinder)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playback = null
  }
}
