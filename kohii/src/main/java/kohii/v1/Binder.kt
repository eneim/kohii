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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.google.android.exoplayer2.PlaybackParameters
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.v1.Playable.RepeatMode
import kohii.v1.Playback.Callback
import kohii.v1.Playback.Controller

class Binder<OUTPUT : Any>(
  private val kohii: Kohii,
  private val media: Media,
  private val playableCreator: PlayableCreator<OUTPUT>
) {

  data class Params(
      // Playable.Config
    var tag: String? = null,
    var prefetch: Boolean = false,
    @RepeatMode var repeatMode: Int = Playable.REPEAT_MODE_OFF,
    var parameters: PlaybackParameters = PlaybackParameters.DEFAULT,

    var delay: Int = 0,
      // Indicator to used to judge of a Playback should be played or not.
      // This doesn't warranty that it will be played, it just to make the Playback be a candidate
      // to start a playback.
      // In ViewPlayback, this is equal to visible area offset of the video container View.
    var threshold: Float = 0.65F,
    var controller: Controller? = null,
    var playbackInfo: PlaybackInfo? = null,
    var keepScreenOn: Boolean = true,
    var callback: Callback? = null
  ) {

    internal fun createPlayableConfig(): Playable.Config {
      return Playable.Config(this.tag, this.prefetch, this.repeatMode, this.parameters)
    }

    internal fun createPlaybackConfig(): Playback.Config {
      return Playback.Config(delay, threshold, controller, playbackInfo, keepScreenOn, callback)
    }
  }

  @RestrictTo(LIBRARY) // don't touch this.
  val params = Params()

  inline fun with(params: Params.() -> Unit): Binder<OUTPUT> {
    this.params.apply(params)
    return this
  }

  fun <CONTAINER : Any> bind(
    target: Target<CONTAINER, OUTPUT>,
    onDone: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = this.params.tag
    val playable = requestPlayable()
    playable.bind(target, this.params.createPlaybackConfig(), onDone)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  fun <CONTAINER : Any> bind(
    target: CONTAINER,
    callback: ((Playback<OUTPUT>) -> Unit)? = null
  ): Rebinder? {
    val tag = this.params.tag
    val playable = requestPlayable()
    playable.bind(target, this.params.createPlaybackConfig(), callback)
    return if (tag != null) Rebinder(tag, playableCreator.outputHolderType) else null
  }

  private fun requestPlayable(): Playable<OUTPUT> {
    val config = this.params.createPlayableConfig()
    val tag = config.tag
    val toCreate: Playable<OUTPUT> by lazy {
      this.playableCreator.createPlayable(kohii, media, this.params.createPlayableConfig())
    }

    val playable =
      if (tag != null) {
        val cache = kohii.mapTagToPlayable[tag]
        if (cache?.second !== playableCreator.outputHolderType) {
          // cached Playable of different output type will be replaced.
          toCreate.also {
            kohii.mapTagToPlayable[tag] = Pair(it, playableCreator.outputHolderType)
            cache?.first?.release()
          }
        } else {
          @Suppress("UNCHECKED_CAST")
          cache.first as Playable<OUTPUT>
        }
      } else {
        toCreate
      }

    kohii.mapPlayableToManager[playable] = null
    return playable
  }
}
