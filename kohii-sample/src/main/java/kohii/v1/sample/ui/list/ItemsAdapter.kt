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

package kohii.v1.sample.ui.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.transition.TransitionSet
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.PlayerInfo
import kohii.v1.sample.R
import kohii.v1.sample.common.InitData
import kohii.v1.sample.ui.list.BaseViewHolder.OnClickListener
import kohii.v1.sample.ui.list.data.Item
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/07/06).
 */
class ItemsAdapter(
  private val kohii: Kohii,
  private val fragment: VerticalListRecyclerViewFragment,
  private val items: List<Item>,
  private val dp2Px: (Int) -> Int
) : Adapter<BaseViewHolder>() {

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_text_view -> TextViewHolder(parent, this.dp2Px)
      R.layout.holder_player_view -> VideoViewHolder(
        parent,
        kohii,
        VideoClickImpl(fragment)
      )

      else -> throw RuntimeException("Unknown type: $viewType")
    }
  }

  override fun getItemCount() = items.size

  override fun getItemId(position: Int): Long {
    val item = items[position]
    return item.hashCode()
      .toLong()
  }

  override fun getItemViewType(position: Int): Int {
    val item = items[position]
    return when (item.type) {
      1 -> R.layout.holder_text_view
      2 -> R.layout.holder_player_view
      else -> throw RuntimeException("Unknown type.")
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(items[position % items.size])
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    holder.onRecycled(true)
  }

  override fun onFailedToRecycleView(holder: BaseViewHolder): Boolean {
    holder.onRecycled(false)
    return true
  }

  class VideoClickImpl(private val fragment: VerticalListRecyclerViewFragment) : OnClickListener {
    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onItemClick(
      itemView: View,
      transView: View?,
      adapterPos: Int,
      payload: Any
    ) {
      if (transView == null) return
      val transName = ViewCompat.getTransitionName(transView) ?: return

      @Suppress("UNCHECKED_CAST")
      val data = payload as? Pair<Rebinder, InitData> ?: return
      val initData = data.second

      fragment.recordPlayerInfo(PlayerInfo(adapterPos, itemView.top))
      // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
      // instead of fading out with the rest to prevent an overlapping animation of fade and move).
      (fragment.exitTransition as TransitionSet).excludeTarget(itemView, true)
      fragment.parentFragmentManager.beginTransaction()
        .setReorderingAllowed(true) // Optimize for shared element transition
        .addSharedElement(transView, transName)
        .replace(
          R.id.fragmentContainer,
          PlayerFragment.newInstance(data.first, initData),
          initData.tag
        )
        .addToBackStack(null)
        .commit()
    }

    override fun onItemLoaded(
      itemView: View,
      adapterPos: Int
    ) {
      val playerInfo = fragment.fetchPlayerInfo()
      if (playerInfo != null && adapterPos != playerInfo.adapterPos) return
      if (enterTransitionStarted.getAndSet(true)) return
      fragment.recordPlayerInfo(null)
      fragment.startPostponedEnterTransition()
    }

    override fun onItemLoadFailed(
      adapterPos: Int,
      error: Exception
    ) {
      if (enterTransitionStarted.getAndSet(true)) return
      fragment.recordPlayerInfo(null)
      fragment.startPostponedEnterTransition()
    }
  }
}
