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

package kohii.v1.sample.ui.main

import android.view.ViewGroup
import android.widget.TextView
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

class DemoItemViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_main_demo_item) {

  private val demoTitle: TextView = itemView.findViewById(R.id.demoTitle)
  private val demoSubtitle: TextView = itemView.findViewById(R.id.demoSubtitle)

  override fun bind(item: Any?) {
    (item as? DemoItem)?.also {
      if (it.title != 0) demoTitle.setText(it.title) else demoTitle.text =
        it.fragmentClass.simpleName
      if (it.subTitle != 0) demoSubtitle.setText(it.subTitle) else demoSubtitle.text = "Debug"
    }
  }
}
