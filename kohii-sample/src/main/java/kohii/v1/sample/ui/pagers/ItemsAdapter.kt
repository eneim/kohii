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

package kohii.v1.sample.ui.pagers

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.android.exoplayer2.Player
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.common.BaseViewHolder

internal class ItemsAdapter(
  private val kohii: Kohii,
  private val pagePos: Int
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
    return if (viewType == TYPE_VIDEO) VideoViewHolder(parent, pagePos)
    else TextViewHolder(parent)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE / 2
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
      holder.videoUrl = assetVideoUri
      val videoTag = holder.videoTag
      kohii.setUp(assetVideoUri) {
        tag = requireNotNull(videoTag)
        repeatMode = Player.REPEAT_MODE_ONE
        artworkHintListener = holder
      }
          .bind(holder.container)
    } else holder.bind(position)
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    if (holder is VideoViewHolder) holder.videoUrl = null
  }
}
