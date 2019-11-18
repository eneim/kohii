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

import android.text.Html
import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

class DemoItemsAdapter(
  val items: List<DemoItem>,
  private val onClick: (DemoItem) -> Unit
) : Adapter<BaseViewHolder>() {

  override fun getItemViewType(position: Int): Int {
    return if (position <= 0) R.layout.holder_main_text else R.layout.holder_main_demo_item
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return when (viewType) {
      R.layout.holder_main_text -> TextViewHolder(parent)
      R.layout.holder_main_demo_item -> DemoItemViewHolder(parent).also { holder ->
        holder.itemView.setOnClickListener {
          onClick(items[holder.adapterPosition - 1])
        }
      }
      else -> throw IllegalArgumentException("Unknown type: $viewType")
    }
  }

  override fun getItemCount() = items.size + 1

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    if (position <= 0) {
      val content = holder.getString(R.string.lib_intro)
          .parseAsHtml(Html.FROM_HTML_MODE_COMPACT)
      holder.bind(content)
    } else {
      holder.bind(items[position - 1])
    }
  }
}
