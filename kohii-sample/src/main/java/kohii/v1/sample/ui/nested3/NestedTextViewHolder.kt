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
import android.widget.TextView
import androidx.core.text.parseAsHtml
import kohii.v1.sample.R.layout
import kohii.v1.sample.R.string
import kohii.v1.sample.common.BaseViewHolder

internal class NestedTextViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, layout.widget_simple_textview) {

  override fun bind(item: Any?) {
    super.bind(item)
    (itemView as TextView).text = itemView.context.getString(
      string.lib_intro
    )
      .parseAsHtml()
  }
}
