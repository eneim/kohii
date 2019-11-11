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

package kohii.core

import android.os.Parcelable
import android.view.ViewGroup
import com.google.android.exoplayer2.Player
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.RawValue

abstract class Rebinder<RENDERER : Any>(
  open val tag: @RawValue Any,
  open val rendererType: Class<RENDERER>
) : Parcelable {

  class Options {
    var repeatMode: Int = Player.REPEAT_MODE_OFF
    var controller: Playback.Controller? = null
    var callbacks: Array<Playback.Callback> = emptyArray()
  }

  @IgnoredOnParcel var options = Options()

  inline fun with(options: Options.() -> Unit): Rebinder<RENDERER> {
    this.options.apply(options)
    return this
  }

  fun <CONTAINER : ViewGroup> bind(
    master: Master,
    container: CONTAINER,
    callback: ((Playback<*>) -> Unit)? = null
  ) {
    val playable = master.playables.asSequence()
        .filter { it.value == tag }
        .firstOrNull()
        ?.key
    require(playable != null && this.rendererType.isAssignableFrom(playable.rendererType))
    master.bind(playable, tag, container, Binder.Options().also {
      it.repeatMode = options.repeatMode
      it.controller = options.controller
      it.callbacks = options.callbacks
    }, callback)
    options = Options() // reset.
  }
}
