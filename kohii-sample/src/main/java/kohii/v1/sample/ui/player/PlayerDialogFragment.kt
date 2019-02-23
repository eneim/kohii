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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.Playback
import kohii.v1.Playback.Callback
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.fragment_player.playerContainer
import kotlinx.android.synthetic.main.fragment_player.playerView

class PlayerDialogFragment : AppCompatDialogFragment(), ContainerProvider, Callback {

  companion object {
    private const val KEY_PLAYABLE_TAG = "kohii:player:dialog:tag"
    private const val KEY_INIT_DATA = "kohii::player::init_data"

    fun newInstance(
      tag: String,
      initData: InitData
    ): PlayerDialogFragment {
      val bundle = Bundle().also {
        it.putString(KEY_PLAYABLE_TAG, tag)
        it.putParcelable(KEY_INIT_DATA, initData)
      }
      return PlayerDialogFragment().also { it.arguments = bundle }
    }
  }

  // Interface to tell ParentFragment about status of this Dialog.
  interface Callback {

    fun onDialogActive(tag: Any)

    fun onDialogInActive(tag: Any)
  }

  val kohii: Kohii by lazy { Kohii[requireContext()] }
  var playback: Playback<*>? = null

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
    val initData = arguments?.getParcelable<InitData>(KEY_INIT_DATA)
    (playerContainer as AspectRatioFrameLayout).setAspectRatio(initData!!.aspectRatio)
  }

  override fun onStart() {
    super.onStart()
    val playableTag = arguments?.getString(KEY_PLAYABLE_TAG) as String
    @Suppress("UNCHECKED_CAST")
    (kohii.findPlayable(playableTag) as? Playable<PlayerView>)
        ?.bind(this, playerView, Playback.PRIORITY_NORMAL) {
          it.addCallback(this)
          playback = it
        }
  }

  override fun onActive(playback: Playback<*>) {
    (parentFragment as? Callback)?.onDialogActive(playback.tag)
  }

  // Would be called after onStop()
  override fun onInActive(playback: Playback<*>) {
    (parentFragment as? Callback)?.onDialogInActive(playback.tag)
  }

  override fun onStop() {
    super.onStop()
    playback?.removeCallback(this@PlayerDialogFragment)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    playback = null
  }

  override fun provideContainers(): Array<Any>? {
    return arrayOf(playerContainer)
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}