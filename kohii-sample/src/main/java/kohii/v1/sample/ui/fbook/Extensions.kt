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

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder

inline fun <reified T : ViewHolder> RecyclerView.filterVisibleHolder(
  predicate: (T) -> Boolean = { true }
): List<T> {
  val layout: LayoutManager = layoutManager ?: return emptyList()
  val childCount = layout.childCount
  if (childCount == 0) return emptyList()
  val result = ArrayList<T>()
  for (i in 0 until childCount) {
    val view = layout.getChildAt(i)
    if (view != null) {
      val holder = this.findContainingViewHolder(view)
      if (holder is T && predicate(holder)) result.add(holder)
    }
  }
  return result
}
