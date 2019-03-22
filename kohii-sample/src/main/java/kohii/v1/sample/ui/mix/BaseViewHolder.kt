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

package kohii.v1.sample.ui.mix

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * @author eneim (2018/07/06).
 */
abstract class BaseViewHolder(
  val inflater: LayoutInflater,
  layoutRes: Int,
  val parent: ViewGroup
) : ViewHolder(inflater.inflate(layoutRes, parent, false)) {

  abstract fun bind(item: Item?)

  open fun onRecycled(success: Boolean) {}

  override fun toString(): String {
    return "${javaClass.simpleName} -- $adapterPosition"
  }
}
