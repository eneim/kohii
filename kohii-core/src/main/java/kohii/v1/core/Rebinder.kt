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

package kohii.v1.core

import android.os.Parcelable
import android.view.ViewGroup
import kohii.v1.core.Master.Companion.NO_TAG
import kohii.v1.core.Playback.ArtworkHintListener
import kohii.v1.core.Playback.Callback
import kohii.v1.core.Playback.Controller
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Rebinder(val tag: @RawValue Any) : Parcelable {

  init {
    require(tag != NO_TAG) { "Rebinder requires unique tag." }
  }

  class Options {
    var threshold: Float = 0.65F
    var preload: Boolean = false
    var repeatMode: Int = Common.REPEAT_MODE_OFF
    var controller: Controller? = null
    var artworkHintListener: ArtworkHintListener? = null
    val callbacks = mutableSetOf<Callback>()
  }

  @JvmSynthetic
  @PublishedApi
  @IgnoredOnParcel
  internal var options = Options()

  inline fun with(options: Options.() -> Unit): Rebinder {
    this.options.apply(options)
    return this
  }

  fun bind(
    engine: Engine<*>,
    container: ViewGroup,
    callback: ((Playback) -> Unit)? = null
  ) {
    this.bind(engine.master, container, callback)
  }

  private fun bind(
    master: Master,
    container: ViewGroup,
    callback: ((Playback) -> Unit)? = null
  ) {
    val playable = master.playables.asSequence()
        .firstOrNull { it.value == tag /* equals */ }
        ?.key
    master.bind(
        requireNotNull(playable) { "Playable is null for tag $tag" }, tag, container,
        Binder.Options().also {
          it.tag = tag
          it.threshold = options.threshold
          it.preload = options.preload
          it.repeatMode = options.repeatMode
          it.controller = options.controller
          it.artworkHintListener = options.artworkHintListener
          it.callbacks += options.callbacks
        }, callback
    )
    options = Options() // reset.
  }
}
