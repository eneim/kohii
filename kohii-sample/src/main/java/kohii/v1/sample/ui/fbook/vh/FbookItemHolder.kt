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

import android.view.View
import android.view.ViewGroup
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

internal abstract class FbookItemHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_fbook_item) {

  interface OnClick {

    fun onClick(receiver: View, holder: FbookItemHolder)
  }

  val textContainer = itemView.findViewById(R.id.textContainer) as ViewGroup
  val photoContainer = itemView.findViewById(R.id.photoContainer) as ViewGroup
  val videoContainer = itemView.findViewById(R.id.videoContainer) as ViewGroup

  internal open fun setupOnClick(onClick: OnClick) {}
}
