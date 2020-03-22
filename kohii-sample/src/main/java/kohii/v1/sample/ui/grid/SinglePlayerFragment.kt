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

package kohii.v1.sample.ui.grid

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import kohii.v1.core.Playback
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.databinding.HolderPlayerViewBinding
import kotlin.LazyThreadSafetyMode.NONE

class SinglePlayerFragment : AppCompatDialogFragment(), Playback.Callback {

  companion object {

    private const val EXTRA_REBINDER = "${BuildConfig.APPLICATION_ID}::debug::rebinder"

    fun newInstance(rebinder: Rebinder) = SinglePlayerFragment().also {
      val args = Bundle()
      args.putParcelable(EXTRA_REBINDER, rebinder)
      it.arguments = args
    }
  }

  private val kohii: Kohii by lazy(NONE) { Kohii[this] }
  private val binding: HolderPlayerViewBinding get() = requireNotNull(_binding)
  private val rebinder: Rebinder by lazy(NONE) {
    requireNotNull(arguments?.getParcelable<Rebinder>(EXTRA_REBINDER))
  }

  private var _binding: HolderPlayerViewBinding? = null
  private var callback: Callback? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    callback = parentFragment as? Callback
  }

  override fun onDetach() {
    super.onDetach()
    callback = null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = HolderPlayerViewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.playerContainer.setAspectRatio(16 / 9F)
    kohii.register(this)
        .addBucket(binding.playerContainer)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onStart() {
    super.onStart()
    binding.root.post { // Use `post`, so that it will be executed _late_ enough.
      rebinder.with {
        callbacks += this@SinglePlayerFragment
      }.bind(kohii, binding.playerView) {
        kohii.stick(it)
      }
    }
  }

  override fun onActive(playback: Playback) {
    super.onActive(playback)
    callback?.onShown(rebinder)
  }

  override fun onInActive(playback: Playback) {
    super.onInActive(playback)
    kohii.unstick(playback)
    callback?.onDismiss(rebinder)
  }

  interface Callback {

    fun onShown(rebinder: Rebinder)

    fun onDismiss(rebinder: Rebinder)
  }
}
