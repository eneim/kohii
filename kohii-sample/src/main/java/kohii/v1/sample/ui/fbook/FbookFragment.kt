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
import androidx.core.view.doOnLayout
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import kohii.media.VolumeInfo
import kohii.v1.Kohii
import kohii.v1.OnSelectionCallback
import kohii.v1.Playback
import kohii.v1.Rebinder
import kohii.v1.Scope
import kohii.v1.TargetHost.Builder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.currentVisible
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.isLandscape
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder
import kohii.v1.sample.ui.fbook.vh.FbookItemHolder.OnClick
import kohii.v1.sample.ui.fbook.vh.VideoViewHolder
import kohii.v1.sample.ui.player.InitData
import kotlinx.android.synthetic.main.fragment_facebook.recyclerView

/**
 * A demonstration that implements the UX of Facebook Videos.
 */
class FbookFragment : BaseFragment() {

  companion object {
    const val ARG_KEY_REBINDER = "kohii::fbook::arg::rebinder"
    const val PAYLOAD_VOLUME = "kohii::fbook::payload::volume"
    fun newInstance() = FbookFragment()
  }

  val viewModel: FbookViewModel by viewModels()

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
    val rvHost = manager.registerTargetHost(hostBuilder)
    manager.addOnSelectionCallback(object : OnSelectionCallback {
      override fun onSelection(playbacks: Collection<Playback<*>>) {
        Log.w("Kohii::Fb", "selection: $playbacks")
        latestRebinder = kohii.findRebinder(playbacks.firstOrNull()?.tag)
      }
    })

    viewModel.timelineVolumeInfo.observe({ viewLifecycleOwner.lifecycle }) {
      kohii.applyVolumeInfo(it, rvHost, Scope.HOST)
      recyclerView.currentVisible<VideoViewHolder>()
          .forEach { viewHolder ->
            recyclerView.adapter?.notifyItemChanged(viewHolder.adapterPosition, it)
          }
    }

    val videos = getApp().videos
    recyclerView.adapter = FbookAdapter(kohii, videos, onClick = object : OnClick {
      override fun onClick(
        receiver: View,
        holder: FbookItemHolder
      ) {
        if (holder is VideoViewHolder && receiver === holder.volume) {
          val current = viewModel.timelineVolumeInfo.value as VolumeInfo
          viewModel.timelineVolumeInfo.value = VolumeInfo(!current.mute, current.volume)
        }
      }
    })

    // Trick to ensure the order of Playback binding.
    // By doing this, the RecyclerView will finish its layout before the BigPlayerFragment being destroyed (if it exists before).
    // Therefore, the ViewHolder will finish the binding before its Playable is released by the destruction of BigPlayerFragment
    postponeEnterTransition()
    recyclerView.doOnLayout {
      startPostponedEnterTransition()
    }

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
