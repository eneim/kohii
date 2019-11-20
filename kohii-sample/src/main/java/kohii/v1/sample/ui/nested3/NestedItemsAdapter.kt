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

package kohii.v1.sample.ui.nested3

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.core.Manager
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.BaseViewHolder

internal class NestedItemsAdapter(
  val kohii: Kohii,
  val manager: Manager
) : Adapter<BaseViewHolder>() {

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
      TYPE_SCROLL -> NestedScrollViewHolder(kohii, manager, parent)
      TYPE_TEXT -> NestedTextViewHolder(parent)
      TYPE_VIDEO -> NestedVideoViewHolder(kohii, parent)
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
