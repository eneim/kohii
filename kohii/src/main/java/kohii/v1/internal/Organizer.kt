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

package kohii.v1.internal

import kohii.v1.core.Playback
import kotlin.properties.Delegates

// TODO revise the design for this class
internal class Organizer {

  internal var selection: List<Playback> by Delegates.observable(
      initialValue = emptyList(),
      onChange = { _, _, _ ->
      }
  )

  internal fun selectFinal(candidates: Collection<Playback>): Collection<Playback> {
    this.selection = listOfNotNull(candidates.firstOrNull())
    return this.selection
  }

  internal fun deselect(vararg playbacks: Playback): Boolean {
    val temp = ArrayList(this.selection)
    val updated = temp.removeAll(playbacks)
    this.selection = temp
    return updated
  }

  internal fun deselect(playbacks: Collection<Playback>): Boolean {
    return this.deselect(*playbacks.toTypedArray())
  }
}
