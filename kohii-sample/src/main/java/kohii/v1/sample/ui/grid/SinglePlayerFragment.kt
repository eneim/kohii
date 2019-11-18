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
import kohii.core.Master
import kohii.core.Playback
import kohii.core.PlayerViewRebinder
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.R
import kotlinx.android.synthetic.main.holder_player_view.playerContainer
import kotlinx.android.synthetic.main.holder_player_view.playerView
import kotlin.LazyThreadSafetyMode.NONE

class SinglePlayerFragment : AppCompatDialogFragment(), Playback.Callback {

  companion object {

    private const val EXTRA_REBINDER = "${BuildConfig.APPLICATION_ID}::debug::rebinder"

    fun newInstance(rebinder: PlayerViewRebinder) = SinglePlayerFragment().also {
      val args = Bundle()
      args.putParcelable(EXTRA_REBINDER, rebinder)
      it.arguments = args
    }
  }

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
    return inflater.inflate(R.layout.holder_player_view, container, false)
  }

  private lateinit var master: Master
  private val rebinder by lazy(NONE) {
    requireNotNull(arguments?.getParcelable<PlayerViewRebinder>(EXTRA_REBINDER))
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    playerContainer.setAspectRatio(16 / 9F)
    master = Master[this]
    master.register(this)
        .attach(playerContainer)
  }

  override fun onStart() {
    super.onStart()
    rebinder.with { callbacks = arrayOf(this@SinglePlayerFragment) }
        .bind(master, playerView) {
          callback?.onShown(rebinder)
          master.stick(it)
        }
  }

  override fun onInActive(playback: Playback) {
    super.onInActive(playback)
    master.unstick(playback)
    callback?.onDismiss(rebinder)
  }

  interface Callback {

    fun onShown(rebinder: PlayerViewRebinder)

    fun onDismiss(rebinder: PlayerViewRebinder)
  }
}
