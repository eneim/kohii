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

package kohii.v1.sample.ui.nested1

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R.layout
import kohii.v1.sample.common.inflateView

internal class VerticalItemsAdapter(
  private val kohii: Kohii,
  private val preferItemCount: Int = Int.MAX_VALUE / 2
) : Adapter<VerticalItemViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): VerticalItemViewHolder {
    val itemView = parent.inflateView(layout.holder_player_view)
    return VerticalItemViewHolder(itemView)
  }

  override fun getItemCount(): Int {
    return preferItemCount
  }

  override fun onBindViewHolder(
    holder: VerticalItemViewHolder,
    position: Int
  ) {
    holder.container.setAspectRatio(16 / 9F)
    kohii.setUp(assetVideoUri) {
      tag = "NESTED::RV::VER::${holder.absoluteAdapterPosition}"
    }
      .bind(holder.container)
  }
}
