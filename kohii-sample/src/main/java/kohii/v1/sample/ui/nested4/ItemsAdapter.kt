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

package kohii.v1.sample.ui.nested4

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp
import kohii.v1.sample.common.BaseViewHolder

internal class ItemsAdapter(
  private val kohii: Kohii,
  private val preferItemCount: Int = Int.MAX_VALUE / 2
) : Adapter<BaseViewHolder>() {

  companion object {
    private const val TYPE_VIDEO = 1
    private const val TYPE_TEXT = 2
  }

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return if (viewType == TYPE_VIDEO) VideoViewHolder(parent) else TextViewHolder(parent)
  }

  override fun getItemCount(): Int {
    return preferItemCount
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemViewType(position: Int): Int {
    return if (position % 6 == 3) TYPE_VIDEO else TYPE_TEXT
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    if (holder is VideoViewHolder) {
      holder.videoUrl = DemoApp.assetVideoUri
      val videoTag = holder.videoTag
      kohii.setUp(DemoApp.assetVideoUri) {
        tag = requireNotNull(videoTag)
      }
        .bind(holder.container)
    } else {
      holder.bind(position)
    }
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    if (holder is VideoViewHolder) holder.videoUrl = null
  }
}
