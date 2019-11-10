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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.parseAsHtml
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Manager
import kohii.core.Master
import kohii.core.Master.MemoryMode
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.BaseViewHolder
import kotlinx.android.synthetic.main.fragment_debug_nestsv_in_rv.recyclerView

internal class NestedTextViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.widget_simple_textview) {

  override fun bind(item: Any?) {
    super.bind(item)
    (itemView as TextView).text = itemView.context.getString(R.string.lib_intro)
        .parseAsHtml()
  }
}

internal class NestedScrollViewHolder(
  val manager: Manager,
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_debug_nestsv) {

  private val container =
    itemView.findViewById(R.id.scrollViewContainer) as AspectRatioFrameLayout
  private val scrollView = itemView.findViewById(R.id.scrollView) as NestedScrollView
  private val playerView = itemView.findViewById(R.id.playerView) as PlayerView
  private val libIntro = itemView.findViewById(R.id.libIntro) as TextView

  init {
    container.setAspectRatio(4 / 5F)
    (playerView.findViewById(
        com.google.android.exoplayer2.ui.R.id.exo_content_frame
    ) as AspectRatioFrameLayout).setAspectRatio(16 / 9F)
  }

  override fun bind(item: Any?) {
    super.bind(item)
    libIntro.text = itemView.context.getString(R.string.lib_intro)
        .parseAsHtml()
  }

  override fun onAttached() {
    super.onAttached()
    manager.attach(scrollView)
    manager.master.setUp(assetVideoUri)
        .with { tag = "NESTED::NSV::${adapterPosition}" }
        .bind(playerView)
  }

  override fun onDetached() {
    super.onDetached()
    manager.detach(scrollView)
  }
}

internal class NestedVideoViewHolder(
  private val master: Master,
  parent: ViewGroup
) : BaseViewHolder(
    parent, R.layout.holder_player_view
) {

  val container = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout
  val playerView = itemView.findViewById(R.id.playerView) as PlayerView

  override fun bind(item: Any?) {
    super.bind(item)
    container.setAspectRatio(16 / 9F)
    master.setUp(assetVideoUri)
        .with { tag = "NESTED::VID::${adapterPosition}" }
        .bind(playerView)
  }
}

internal class NestedItemsAdapter(val manager: Manager) : Adapter<BaseViewHolder>() {

  companion object {
    const val TYPE_SCROLL = 1
    const val TYPE_TEXT = 2
    const val TYPE_VIDEO = 3
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 5) TYPE_SCROLL else if (position % 3 == 1) TYPE_VIDEO else TYPE_TEXT
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      TYPE_SCROLL -> NestedScrollViewHolder(manager, parent)
      TYPE_TEXT -> NestedTextViewHolder(parent)
      TYPE_VIDEO -> NestedVideoViewHolder(manager.master, parent)
      else -> throw IllegalArgumentException("Unknown type $viewType")
    }
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    holder.bind(position)
  }

  override fun onViewAttachedToWindow(holder: BaseViewHolder) {
    super.onViewAttachedToWindow(holder)
    holder.onAttached()
  }

  override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
    super.onViewDetachedFromWindow(holder)
    holder.onDetached()
  }
}

class NestedScrollViewInsideRecyclerViewFragment : BaseFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug_nestsv_in_rv, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val master = Master[this]
    val manager = master.register(this, MemoryMode.BALANCED)
        .attach(recyclerView)

    recyclerView.adapter = NestedItemsAdapter(manager)

    // To allow NestedScrollView to scroll inside RecyclerView.
    // This implementation is really simple and should not be used as-is in production code.
    recyclerView.addOnItemTouchListener(object : SimpleOnItemTouchListener() {

      override fun onInterceptTouchEvent(
        rv: RecyclerView,
        e: MotionEvent
      ): Boolean {
        val child = rv.findChildViewUnder(e.x, e.y) ?: return false
        val holder = rv.findContainingViewHolder(child) ?: return false
        if (holder !is NestedScrollViewHolder) return false
        child.parent.requestDisallowInterceptTouchEvent(true)
        return false
      }
    })
  }
}
