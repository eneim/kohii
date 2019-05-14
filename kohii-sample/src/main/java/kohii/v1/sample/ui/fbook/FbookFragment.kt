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

package kohii.v1.sample.ui.fbook

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import kohii.v1.Kohii
import kohii.v1.OnSelectionCallback
import kohii.v1.Playback
import kohii.v1.Rebinder
import kohii.v1.TargetHost.Builder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.ui.player.InitData
import kotlinx.android.synthetic.main.fragment_facebook.recyclerView

/**
 * A demonstration that implements the UX of Facebook Videos.
 */
class FbookFragment : BaseFragment() {

  companion object {
    const val ARG_KEY_REBINDER = "kohii::fbook::arg::rebinder"
    fun newInstance() = FbookFragment()
  }

  lateinit var kohii: Kohii
  var latestRebinder: Rebinder? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_facebook, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Kohii[this]
    val manager = kohii.register(this)
    val hostBuilder = Builder(recyclerView)
    manager.registerTargetHost(hostBuilder)
    manager.addOnSelectionCallback(object : OnSelectionCallback {
      override fun onSelection(playbacks: Collection<Playback<*>>) {
        Log.w("Kohii::Fb", "selection: $playbacks")
        latestRebinder = kohii.findRebinder(playbacks.firstOrNull()?.tag)
      }
    })

    val videos = getApp().videos
    recyclerView.adapter = FbookAdapter(kohii, videos)

    val savedBinder = savedInstanceState?.getParcelable(ARG_KEY_REBINDER) as Rebinder?
    if (savedBinder != null && requireActivity().isLandscape()) {
      val player = BigPlayerFragment.newInstance(
          savedBinder,
          InitData(savedBinder.tag, 16 / 9.toFloat())
      )
      fragmentManager!!.commit {
        setReorderingAllowed(true)
        replace(R.id.fragmentContainer, player, savedBinder.tag)
        addToBackStack(null)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (latestRebinder != null) outState.putParcelable(ARG_KEY_REBINDER, latestRebinder)
  }
}
