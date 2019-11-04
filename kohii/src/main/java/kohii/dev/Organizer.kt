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

package kohii.dev

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ListUpdateCallback
import kotlin.properties.Delegates

internal class Organizer {

  internal var selection: List<Playback<*>> by Delegates.observable(
      initialValue = emptyList(),
      onChange = { _, prev, next ->
        DiffUtil.calculateDiff(object : Callback() {
          override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
          ): Boolean {
            return prev[oldItemPosition] === next[newItemPosition]
          }

          override fun getOldListSize() = prev.size

          override fun getNewListSize() = next.size

          override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
          ): Boolean {
            return prev[oldItemPosition] === next[newItemPosition]
          }
        }, false)
            .dispatchUpdatesTo(object : ListUpdateCallback {
              override fun onChanged(
                position: Int,
                count: Int,
                payload: Any?
              ) {
                // Do nothing
              }

              override fun onMoved(
                fromPosition: Int,
                toPosition: Int
              ) {
                // Do nothing
              }

              override fun onInserted(
                position: Int,
                count: Int
              ) {
                // TODO
              }

              override fun onRemoved(
                position: Int,
                count: Int
              ) {
                // TODO
              }
            })
      }
  )

  internal fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    this.selection = listOfNotNull(candidates.firstOrNull())
    return this.selection
  }

  internal fun deselect(vararg playbacks: Playback<*>): Boolean {
    val temp = ArrayList(this.selection)
    val updated = temp.removeAll(playbacks)
    this.selection = temp
    return updated
  }

  internal fun deselect(playbacks: Collection<Playback<*>>): Boolean {
    return this.deselect(*playbacks.toTypedArray())
  }
}
