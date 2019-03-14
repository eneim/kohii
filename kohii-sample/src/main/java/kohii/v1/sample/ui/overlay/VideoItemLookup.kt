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

package kohii.v1.sample.ui.overlay

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import kohii.v1.Rebinder

class VideoItemLookup(val parent: RecyclerView) : ItemDetailsLookup<Rebinder>() {
  override fun getItemDetails(event: MotionEvent): ItemDetails<Rebinder>? {
    val view = parent.findChildViewUnder(event.x, event.y)
    if (view != null) {
      return (parent.getChildViewHolder(view) as VideoItemHolder).getItemDetails()
    }
    return null
  }
}