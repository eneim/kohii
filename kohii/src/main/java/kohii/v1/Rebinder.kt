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
import android.view.ViewGroup
import kohii.v1.Binder.Params
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

// Pass instance of this class around to rebind the Playable.
// This requires the type of next Target must be the same or sub-class of original Target.
@Parcelize
data class Rebinder<RENDERER : Any>(
  val tag: String,
  val rendererType: Class<RENDERER>
) : Parcelable {

  @IgnoredOnParcel
  private var params = Params() // this object is "one shot", mean that once used, will be reset.

  fun with(params: Params.() -> Unit): Rebinder<RENDERER> {
    this.params.apply(params)
    return this
  }

  fun rebind(
    kohii: Kohii,
    target: RENDERER,
    reset: Boolean = false,
    onDone: ((Playback<RENDERER>) -> Unit)? = null
  ): Rebinder<RENDERER> {
    val cache = kohii.mapTagToPlayable[this.tag]
    if (cache != null) {
      // cache.first -> cached Playable instance
      // cache.second -> compatible renderer type of the Playable

      // Also check type of Playable found in cache
      val playable = if (this.rendererType.isAssignableFrom(cache.second)) cache.first else null
      check(playable != null) { "No Playable found for tag ${this.tag}" }
      if (reset) playable.reset()
      @Suppress("UNCHECKED_CAST") // it should be safe, as we've checked container type above.
      (playable as Playable<RENDERER>).bind(
          IdenticalTarget(target) as Target<ViewGroup, RENDERER>,
          this.params.createPlaybackConfig(), onDone
      )
    } else check(!BuildConfig.DEBUG) { "No Playable found for tag ${this.tag}." }
    params = Params()
    return this
  }

  fun <CONTAINER : ViewGroup> rebind(
    kohii: Kohii,
    target: Target<CONTAINER, RENDERER>,
    reset: Boolean = false,
    onDone: ((Playback<RENDERER>) -> Unit)? = null
  ): Rebinder<RENDERER> {
    val cache = kohii.mapTagToPlayable[this.tag]
    if (cache != null) {
      // cache.first -> cached Playable instance
      // cache.second -> compatible renderer type of the Playable

      // Also check type of Playable found in cache
      val playable = if (this.rendererType.isAssignableFrom(cache.second)) cache.first else null
      check(playable != null) { "No Playable found for tag ${this.tag}" }
      if (reset) playable.reset()
      @Suppress("UNCHECKED_CAST") // it should be safe, as we've checked container type above.
      (playable as Playable<RENDERER>).bind(target, this.params.createPlaybackConfig(), onDone)
    } else {
      check(!BuildConfig.DEBUG) { "No Playable found for tag ${this.tag}." }
    }
    params = Params()
    return this
  }
}
