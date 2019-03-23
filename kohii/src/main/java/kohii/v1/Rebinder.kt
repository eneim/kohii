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
import kohii.v1.Playback.Config
import kotlinx.android.parcel.Parcelize

// Pass instance of this class around to rebind the Playable.
// This requires the type of next Target must be the same or sub-class of original Target.
@Parcelize
data class Rebinder(
  val tag: String?,
  val playerType: Class<*>
) : Parcelable {

  fun <TARGET : Any> rebind(
    kohii: Kohii,
    target: TARGET,
    config: Config = Config(), // default
    cb: ((Playback<TARGET, *>) -> Unit)? = null
  ): Rebinder {
    require(this.tag != null) { "Rebinder expects non-null tag." }
    val tag = this.tag
    val cache = kohii.mapTagToPlayable[tag]
    if (cache != null) {
      val playable = if (this.playerType.isAssignableFrom(cache.second)) cache.first else null
      check(playable != null) { "No Playable found. Tag is not correctly set." }
      playable.bind(target, config, cb)
    } else if (BuildConfig.DEBUG) {
      throw IllegalStateException("No Playable found for tag $tag.")
    }
    return this
  }
}
