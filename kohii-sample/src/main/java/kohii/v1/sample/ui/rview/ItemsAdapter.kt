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

package kohii.v1.sample.ui.rview

import androidx.transition.TransitionSet
import androidx.recyclerview.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kohii.v1.sample.R
import kohii.v1.sample.ui.player.PlayerFragment
import kohii.v1.sample.ui.rview.BaseViewHolder.OnClickListener
import kohii.v1.sample.ui.rview.RecyclerViewFragment.PlayerInfo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/07/06).
 */
class ItemsAdapter(
    private val fragment: RecyclerViewFragment,
    private val items: List<Item>,
    private val dp2Px: (Int) -> Int
) : Adapter<BaseViewHolder>() {

  private var inflater: LayoutInflater? = null

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    if (inflater == null || inflater!!.context != parent.context) {
      inflater = LayoutInflater.from(parent.context)
    }

    return when (viewType) {
      R.layout.holder_text_view -> TextViewHolder(inflater!!, parent, this.dp2Px)
      R.layout.holder_player_view -> VideoViewHolder(inflater!!, parent, VideoClickImpl(fragment))
      else -> throw RuntimeException("Unknown type: $viewType")
    }
  }

  override fun getItemCount() = items.size


  override fun getItemId(position: Int): Long {
    val item = items[position]
    return item.hashCode().toLong()
  }

  override fun getItemViewType(position: Int): Int {
    val item = items[position]
    return when (item.type) {
      1 -> R.layout.holder_text_view
      2 -> R.layout.holder_player_view
      else -> throw RuntimeException("Unknown type.")
    }
  }

  override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
    holder.bind(items[position])
  }

  class VideoClickImpl(private val fragment: RecyclerViewFragment) : OnClickListener {
    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onItemClick(itemView: View, transView: View?, adapterPos: Int, payload: Any) {
      if (transView == null) return
      fragment.recordPlayerInfo(PlayerInfo(adapterPos, itemView.top))
      // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
      // instead of fading out with the rest to prevent an overlapping animation of fade and move).
      (fragment.exitTransition as TransitionSet).excludeTarget(itemView, true)
      val tag = payload as String
      fragment.fragmentManager!!.beginTransaction()
          .setReorderingAllowed(true) // Optimize for shared element transition
          .addSharedElement(transView, transView.transitionName)
          .replace(R.id.fragmentContainer, PlayerFragment.newInstance(tag), tag)
          .addToBackStack(null)
          .commit()
    }

    override fun onItemLoaded(itemView: View, adapterPos: Int) {
      val playerInfo = fragment.fetchPlayerInfo()
      if (playerInfo == null || adapterPos != playerInfo.adapterPos) return
      if (enterTransitionStarted.getAndSet(true)) return

      fragment.recordPlayerInfo(null)
      fragment.startPostponedEnterTransition()
    }
  }
}