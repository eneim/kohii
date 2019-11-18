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

package kohii.v1.sample.ui.grid

import android.util.SparseArray
import android.view.View
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import kohii.core.Rebinder

internal class VideoTagKeyProvider(
  val recyclerView: RecyclerView
) : ItemKeyProvider<Rebinder>(SCOPE_CACHED) {

  private val posToKey = SparseArray<Rebinder>()
  private val keyToPos = HashMap<Rebinder, Int>()

  init {
    require(recyclerView.adapter?.hasStableIds() == true)
    recyclerView.addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
      override fun onChildViewDetachedFromWindow(view: View) {
        onDetached(view)
      }

      override fun onChildViewAttachedToWindow(view: View) {
        onAttached(view)
      }
    })
  }

  override fun getKey(position: Int): Rebinder? {
    return posToKey[position]
  }

  override fun getPosition(key: Rebinder): Int {
    return keyToPos[key] ?: RecyclerView.NO_POSITION
  }

  internal fun onAttached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view) as? VideoViewHolder ?: return
    val id = holder.itemId
    if (id != RecyclerView.NO_ID) {
      val position = holder.adapterPosition
      val key = holder.rebinder
      if (position != RecyclerView.NO_POSITION && key != null) {
        posToKey.put(position, key)
        keyToPos[key] = position
      }
    }
  }

  internal fun onDetached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view) as? VideoViewHolder ?: return
    val id = holder.itemId
    // only if id == NO_ID, we remove this View from cache.
    // when id != NO_ID, it means that this View is still bound to an Item.
    if (id == RecyclerView.NO_ID) {
      val position = holder.adapterPosition
      val key = holder.rebinder
      if (position != RecyclerView.NO_POSITION && key != null) {
        posToKey.remove(position)
        keyToPos.remove(key)
      }
    }
  }
}
