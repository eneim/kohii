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

package kohii.v1.sample.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder

open class BaseViewHolder(
  parent: ViewGroup,
  layoutId: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false)) {

  fun getString(@StringRes res: Int): String = itemView.context.getString(res)

  open fun bind(item: Any?) {}

  open fun onRecycled(success: Boolean) {}
}
