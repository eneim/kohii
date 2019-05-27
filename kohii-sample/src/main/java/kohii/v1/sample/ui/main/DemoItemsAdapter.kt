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

package kohii.v1.sample.ui.main

import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder
import kohii.v1.sample.ui.combo.ComboFragment
import kohii.v1.sample.ui.fbook.FbookFragment
import kohii.v1.sample.ui.motion.MotionFragment
import kohii.v1.sample.ui.mstdtl.MasterDetailFragment
import kohii.v1.sample.ui.overlay.OverlayViewFragment
import kohii.v1.sample.ui.pager1.PagerMainFragment
import kohii.v1.sample.ui.rview.RecyclerViewFragment
import kohii.v1.sample.ui.sview.ScrollViewFragment

class DemoItemsAdapter(
  private val onClick: (DemoItem) -> Unit
) : Adapter<BaseViewHolder>() {

  private val items: List<DemoItem>

  init {
    items = arrayListOf(
        DemoItem(
            R.string.demo_title_fbook,
            R.string.demo_desc_fbook,
            FbookFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_master_detail,
            R.string.demo_desc_master_detail,
            MasterDetailFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_1,
            R.string.demo_desc_pager_1,
            PagerMainFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_1,
            R.string.demo_desc_recycler_view_1,
            RecyclerViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_2,
            R.string.demo_desc_recycler_view_2,
            ComboFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_3,
            R.string.demo_desc_recycler_view_3,
            OverlayViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_nested_scrollview_1,
            R.string.demo_desc_nested_scrollview_1,
            MotionFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_nested_scrollview_2,
            R.string.demo_desc_nested_scrollview_2,
            ScrollViewFragment::class.java
        )
    )
  }

  override fun getItemViewType(position: Int): Int {
    return if (position <= 0) R.layout.holder_main_text else R.layout.holder_main_demo_item
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_main_text -> TextViewHolder(parent)
      R.layout.holder_main_demo_item -> DemoItemViewHolder(parent).also {
        it.itemView.setOnClickListener { _ -> onClick(items[it.adapterPosition - 1]) }
      }
      else -> throw IllegalArgumentException("Unsupported type: $viewType")
    }
  }

  override fun getItemCount() = items.size + 1

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    if (position <= 0) {
      val content =
        HtmlCompat.fromHtml(holder.getString(R.string.lib_intro), FROM_HTML_MODE_COMPACT)
      holder.bind(content)
    } else {
      holder.bind(items[position - 1])
    }
  }
}
