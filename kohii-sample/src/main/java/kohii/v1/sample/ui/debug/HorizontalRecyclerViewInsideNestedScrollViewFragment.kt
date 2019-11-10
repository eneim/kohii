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
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.core.Master
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.inflateView
import kohii.v1.sample.ui.manual.videoUrl
import kotlinx.android.synthetic.main.fragment_debug_rv_in_nestsv_horizontal.libIntro
import kotlinx.android.synthetic.main.fragment_debug_rv_in_nestsv_horizontal.recyclerView
import kotlinx.android.synthetic.main.fragment_debug_rv_in_nestsv_horizontal.scrollView

internal class HorizontalItemViewHolder(itemView: View) : ViewHolder(itemView) {

  internal val container = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
}

internal class HorizontalItemsAdapter(private val master: Master) : Adapter<HorizontalItemViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): HorizontalItemViewHolder {
    val itemView = parent.inflateView(R.layout.holder_player_view)
    return HorizontalItemViewHolder(itemView)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: HorizontalItemViewHolder,
    position: Int
  ) {
    holder.container.setAspectRatio(16 / 9F)
    master.setUp(videoUrl)
        .with { tag = "NESTED::RV::HOZ::${holder.adapterPosition}" }
        .bind(holder.container)
  }
}

class HorizontalRecyclerViewInsideNestedScrollViewFragment : BaseFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_rv_in_nestsv_horizontal, container, false)
  }

  lateinit var snapHelper: PagerSnapHelper

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val master = Master[this]
    master.register(this)
        .attach(scrollView, recyclerView)

    libIntro.text = getString(R.string.lib_intro).parseAsHtml()

    recyclerView.adapter = HorizontalItemsAdapter(master)
    snapHelper = PagerSnapHelper()
    snapHelper.attachToRecyclerView(recyclerView)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    snapHelper.attachToRecyclerView(null)
  }
}
