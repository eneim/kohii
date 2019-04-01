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

package kohii.v1.sample.ui.rview

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kohii.v1.sample.R
import kohii.v1.sample.svg.GlideApp
import kohii.v1.sample.svg.SvgSoftwareLayerSetter
import kohii.v1.sample.ui.rview.data.Item

/**
 * @author eneim (2018/07/06).
 */
@Suppress("MemberVisibilityCanBePrivate", "DEPRECATION")
class TextViewHolder(
  inflater: LayoutInflater,
  parent: ViewGroup,
  private val dp2Px: (Int) -> Int
) : BaseViewHolder(inflater, R.layout.holder_text_view, parent) {

  val textView = itemView.findViewById(R.id.contentView) as TextView
  val iconView = itemView.findViewById(R.id.iconView) as ImageView

  @SuppressLint("PrivateResource")
  override fun bind(item: Item?) {
    itemView.isVisible = item != null

    if (item !== null) {
      itemView.setBackgroundColor(Color.parseColor(item.background))
      iconView.isVisible = item.icon != null
      textView.text = item.content
      val textApp =
        if (item.format == "text:title") R.style.TextAppearance_AppCompat_Large else R.style.TextAppearance_AppCompat_Medium
      textView.setTextAppearance(textView.context, textApp)
      val textColor =
        if (item.background == "#ffffff") R.color.abc_primary_text_material_light else R.color.abc_primary_text_material_dark
      textView.setTextColor(ContextCompat.getColor(itemView.context, textColor))

      if (item.icon != null) {
        GlideApp.with(itemView)
            .`as`(PictureDrawable::class.java)
            .listener(SvgSoftwareLayerSetter())
            .load(item.icon.url)
            .override(dp2Px(item.icon.width), dp2Px(item.icon.height))
            .into(iconView)
      }
    }
  }
}
