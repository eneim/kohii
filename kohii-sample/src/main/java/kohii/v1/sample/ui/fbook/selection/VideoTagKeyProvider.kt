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

package kohii.v1.sample.ui.fbook.selection

import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.core.util.putAll
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import kohii.v1.Rebinder
import kohii.v1.sample.ui.fbook.vh.VideoViewHolder
import java.util.HashMap

// This KeyProvider allow a detached View still in key/position map.
class VideoTagKeyProvider(private val recyclerView: RecyclerView) :
    ItemKeyProvider<Rebinder>(SCOPE_CACHED) {

  companion object {
    const val KEY_STATE_KEY = "kohii::fbook::state::key_provider::key"
    const val KEY_STATE_POS = "kohii::fbook::state::key_provider::pos"
  }

  private val positionToKey = SparseArray<Rebinder>()
  private val keyToPosition = HashMap<Rebinder, Int>()

  init {
    recyclerView.addOnChildAttachStateChangeListener(
        object : OnChildAttachStateChangeListener {
          override fun onChildViewAttachedToWindow(view: View) {
            onAttached(view)
          }

          override fun onChildViewDetachedFromWindow(view: View) {
            onDetached(view)
          }
        }
    )
  }

  internal /* synthetic access */ fun onAttached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view)
    if (holder is VideoViewHolder) {
      val position = holder.adapterPosition
      val id = holder.getItemId()
      if (id != NO_ID) {
        val key = holder.rebinder
        if (position != NO_POSITION && key != null) {
          positionToKey.put(position, key)
          keyToPosition[key] = position
        }
      }
    }
  }

  internal /* synthetic access */ fun onDetached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view)
    if (holder is VideoViewHolder) {
      val position = holder.adapterPosition
      val id = holder.getItemId()
      // only if id == NO_ID, we remove this View from cache.
      // when id != NO_ID, it means that this View is still bound to an Item.
      if (id == NO_ID) {
        val key = holder.rebinder
        if (position != NO_POSITION && key != null) {
          positionToKey.delete(position)
          keyToPosition.remove(key)
        }
      }
    }
  }

  override fun getKey(position: Int): Rebinder? {
    return positionToKey.get(position, null)
  }

  override fun getPosition(key: Rebinder): Int {
    return keyToPosition[key] ?: NO_POSITION
  }

  fun saveToState(outState: Bundle) {
    outState.putSerializable(KEY_STATE_POS, this.keyToPosition)
    outState.putSparseParcelableArray(KEY_STATE_KEY, this.positionToKey)
  }

  fun restoreFromState(state: Bundle) {
    this.keyToPosition.clear()
    @Suppress("UNCHECKED_CAST")
    this.keyToPosition.putAll(state.getSerializable(KEY_STATE_POS) as Map<out Rebinder, Int>)
    this.positionToKey.clear()
    this.positionToKey.putAll(state.getSparseParcelableArray(KEY_STATE_KEY)!!)
  }
}
