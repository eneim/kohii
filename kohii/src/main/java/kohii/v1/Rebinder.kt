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

import android.os.Parcelable
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Playback.Config
import kotlinx.android.parcel.Parcelize

// Pass instance of this class around to rebind the Playable.
// This requires the type of next Target must be the same or sub-class of original Target.
@Parcelize
data class Rebinder(
  val tag: String?,
  val clazz: Class<*>
) : Parcelable {

  fun <TARGET : Any> rebind(
    kohii: Kohii,
    target: TARGET,
    config: Config = Config(), // default
    cb: ((Playback<TARGET, PlayerView>) -> Unit)? = null
  ): Rebinder {
    val targetType = target.javaClass
    if (this.tag != null && clazz.isAssignableFrom(targetType)) {
      @Suppress("UNCHECKED_CAST")
      val playable = (kohii.mapTagToPlayable[this.tag] as? Playable<PlayerView>)
      check(playable != null) { "No Playable found. Tag is not correctly set." }
      playable.bind(target, config, cb)
    } else {
      throw IllegalArgumentException("Expect: ${clazz.simpleName}, found ${targetType.simpleName}")
    }
    return this
  }
}