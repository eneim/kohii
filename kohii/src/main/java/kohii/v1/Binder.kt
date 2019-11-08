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

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.google.android.exoplayer2.PlaybackParameters
import kohii.media.Media
import kohii.media.PlaybackInfo
import kohii.v1.Playable.RepeatMode
import kohii.v1.Playback.Callback
import kohii.v1.Playback.Controller
import kohii.v1.exo.DefaultExoPlayerConfig
import java.util.concurrent.Future
import kotlin.LazyThreadSafetyMode.NONE

open class Binder<RENDERER : Any> internal constructor(
  private val kohii: Kohii,
  private val media: Media,
  private val playableCreator: PlayableCreator<RENDERER>
) {

  data class Params(
      // Playable.Config
    var tag: String? = null,
    var preLoad: Boolean = false,
    var cover: Future<Bitmap?>? = null,
      // Playback.Config
    @RepeatMode var repeatMode: Int = Playable.REPEAT_MODE_OFF,
    var parameters: PlaybackParameters = DefaultExoPlayerConfig.PLAYBACK_PARAMS,
    var delay: Int = 0,
      // Indicator to judge if a Playback should be played or not.
      // This doesn't make sure that it will be played, it just to make the Playback be a candidate
      // to start a playback.
      // In ViewPlayback, this is equal to visible area offset of the video container View.
    var threshold: Float = 0.65F,
    var disabled: () -> Boolean = { false },
    var controller: Controller? = null,
    var playbackInfo: PlaybackInfo? = null,
    var keepScreenOn: Boolean = true,
    var callback: Callback? = null,
    var headlessPlaybackParams: HeadlessPlaybackParams? = null
  ) {

    internal fun createPlayableConfig(): Playable.Config {
      return Playable.Config(
          this.tag,
          this.preLoad,
          this.cover,
          this.headlessPlaybackParams
      )
    }

    internal fun createPlaybackConfig(): Playback.Config {
      return Playback.Config(
          delay,
          threshold,
          disabled,
          controller,
          playbackInfo,
          repeatMode,
          parameters,
          keepScreenOn,
          callback
      )
    }
  }

  // Made public for the inline DSL function.
  @RestrictTo(LIBRARY) // don't touch this.
  val params = Params()

  inline fun with(params: Params.() -> Unit): Binder<RENDERER> {
    this.params.apply(params)
    return this
  }

  fun <CONTAINER : ViewGroup> bind(
    target: Target<CONTAINER, RENDERER>,
    onDone: ((Playback<RENDERER>) -> Unit)? = null
  ): Rebinder<RENDERER>? {
    val tag = this.params.tag
    val playable = requestPlayable(this.params.createPlayableConfig())
    playable.bind(target, this.params.createPlaybackConfig(), onDone)
    return if (tag != null) Rebinder(tag, playableCreator.rendererType) else null
  }

  fun bind(
    renderer: RENDERER,
    onDone: ((Playback<RENDERER>) -> Unit)? = null
  ): Rebinder<RENDERER>? {
    require(renderer is ViewGroup) { "Target type is not allowed here." }
    @Suppress("UNCHECKED_CAST")
    return this.bind(IdenticalTarget(renderer) as Target<ViewGroup, RENDERER>, onDone)
  }

  private fun requestPlayable(config: Playable.Config): Playable<RENDERER> {
    val tag = config.tag
    val toCreate: Playable<RENDERER> by lazy(NONE) {
      this.playableCreator.createPlayable(kohii, media, config)
    }

    val playable =
      if (tag != null) {
        val cache = kohii.mapTagToPlayable[tag]
        if (cache?.second !== playableCreator.rendererType) {
          // cached Playable of different renderer type will be replaced.
          if (cache?.first != null) kohii.releasePlayable(tag, cache.first) // Added [20190726]
          toCreate.also {
            kohii.mapTagToPlayable[tag] = it to playableCreator.rendererType
          }
        } else {
          @Suppress("UNCHECKED_CAST")
          cache.first as Playable<RENDERER>
        }
      } else {
        toCreate
      }

    playable.manager = null
    return playable
  }
}
