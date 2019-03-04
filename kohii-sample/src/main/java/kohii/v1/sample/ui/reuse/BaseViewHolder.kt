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

package kohii.v1.sample.ui.reuse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * @author eneim (2018/07/06).
 */
abstract class BaseViewHolder(
  inflater: LayoutInflater,
  layoutRes: Int,
  parent: ViewGroup
) : ViewHolder(
    inflater.inflate(layoutRes, parent, false)
) {

  open fun bind(item: Any?) {}

  open fun onRecycled(success: Boolean) {}

  interface OnClickListener {

    fun onItemClick(
      itemView: View,   // The main View receives the click
      transView: View?, // The view to use in SharedElement Transition.
      adapterPos: Int,  // The adapter position.
      itemId: Long,
      payload: Any?      // Payload, for Video it is the tag (String), used as Transition name.
    )
  }

  override fun toString(): String {
    return javaClass.simpleName + " -- " + adapterPosition
  }
}