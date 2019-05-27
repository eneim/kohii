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

package kohii.v1.sample.ui.fbook.vh

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import kohii.v1.sample.R

internal class PhotoViewHolder(parent: ViewGroup) : FbookItemHolder(parent) {
  init {
    photoContainer.isVisible = true
  }

  private val photoContent = itemView.findViewById(R.id.photoContent) as ImageView
  private val photoCaption = itemView.findViewById(R.id.photoCaption) as TextView

  override fun bind(item: Any?) {
    super.bind(item)
    photoCaption.text = itemView.context.getText(R.string.large_text)
    Glide.with(photoContent)
        .load("https://content.jwplatform.com/thumbs/0G7vaSoF-720.jpg")
        .thumbnail(0.15F)
        .fitCenter()
        .into(photoContent)
  }
}
