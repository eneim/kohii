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

package kohii.v1

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ListUpdateCallback
import kohii.logDebug
import kotlin.properties.Delegates

interface PlaybackOrganizer {

  // Never update this list, always mutate.
  var selection: List<Playback<*>>

  fun select(candidates: Collection<Playback<*>>): List<Playback<*>>

  fun deselect(vararg playbacks: Playback<*>): Boolean

  fun deselect(playbacks: Collection<Playback<*>>): Boolean
}

internal class SinglePlayerOrganizer : PlaybackOrganizer {

  override var selection: List<Playback<*>> by Delegates.observable(
      emptyList(),
      onChange = { _, oldList, newList ->
        DiffUtil.calculateDiff(object : Callback() {
          override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
          ): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition]
          }

          override fun getOldListSize(): Int {
            return oldList.size
          }

          override fun getNewListSize(): Int {
            return newList.size
          }

          override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
          ): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition]
          }
        }, false)
            .dispatchUpdatesTo(object : ListUpdateCallback {
              override fun onChanged(
                position: Int,
                count: Int,
                payload: Any?
              ) {
                // Nothing to do
              }

              override fun onMoved(
                fromPosition: Int,
                toPosition: Int
              ) {
                // Nothing to do
              }

              override fun onInserted(
                position: Int,
                count: Int
              ) {
                "DIFF :: Inserted ${newList[position]}".logDebug()
              }

              override fun onRemoved(
                position: Int,
                count: Int
              ) {
                "DIFF :: Removed ${oldList[position]}".logDebug()
              }
            })
      })

  override fun select(candidates: Collection<Playback<*>>): List<Playback<*>> {
    this.selection = listOfNotNull(candidates.firstOrNull())
    return this.selection
  }

  override fun deselect(vararg playbacks: Playback<*>): Boolean {
    val temp = ArrayList(this.selection)
    val removed = temp.removeAll(playbacks)
    this.selection = temp
    return removed
  }

  override fun deselect(playbacks: Collection<Playback<*>>): Boolean {
    val temp = ArrayList(this.selection)
    val removed = temp.removeAll(playbacks)
    this.selection = temp
    return removed
  }
}
