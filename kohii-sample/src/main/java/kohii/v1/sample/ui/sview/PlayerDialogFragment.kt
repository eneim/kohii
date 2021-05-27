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
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.InitData
import kohii.v1.sample.databinding.FragmentPlayerBinding

class PlayerDialogFragment : AppCompatDialogFragment(), Playback.Callback {

  companion object {
    private const val KEY_INIT_DATA = "kohii::player::init_data"
    private const val KEY_REBINDER = "kohii:player:dialog:rebinder"

    fun newInstance(
      rebinder: Rebinder,
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

    fun onDialogInActive(rebinder: Rebinder)
  }

  private lateinit var kohii: Kohii
  private lateinit var rebinder: Rebinder
  private lateinit var binding: FragmentPlayerBinding

  private var playback: Playback? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    (dialog as? AppCompatDialog)?.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    return dialog
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding: FragmentPlayerBinding = FragmentPlayerBinding.inflate(inflater, container, false)
    this.binding = binding
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val initData = requireNotNull(requireArguments().getParcelable<InitData>(KEY_INIT_DATA))
    binding.playerContainer.setAspectRatio(initData.aspectRatio)

    kohii = Kohii[this].also {
      it.register(this)
          .addBucket(binding.playerContainer)
    }
  }

  override fun onStart() {
    super.onStart()
    val rebinder: Rebinder = requireNotNull(requireArguments().getParcelable(KEY_REBINDER))
    rebinder.with {
      callbacks += this@PlayerDialogFragment
    }
        .bind(kohii, binding.playerView) { playback = it }
    this.rebinder = rebinder
  }

  override fun onActive(playback: Playback) {
    (parentFragment as? Callback)?.onDialogActive()
  }

  // Would be called after onStop()
  override fun onInActive(playback: Playback) {
    (parentFragment as? Callback)?.onDialogInActive(rebinder)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playback = null
  }
}
