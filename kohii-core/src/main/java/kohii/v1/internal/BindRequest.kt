/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kohii.v1.core.Binder.Options
import kohii.v1.core.Bucket
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playback
import kohii.v1.core.Playback.Config
import kohii.v1.logInfo
import kohii.v1.logWarn

internal class BindRequest(
  val master: Master,
  val playable: Playable,
  val container: ViewGroup,
  val tag: Any,
  val options: Options,
  val callback: ((Playback) -> Unit)?
) {

  // Used by RecyclerViewBucket to 'assume' that it will hold this container
  // It is recommended to use Engine#cancel to easily remove a queued request from cache.
  internal var bucket: Bucket? = null

  internal fun onBind() {
    val bucket = master.findBucketForContainer(container)

    requireNotNull(bucket) { "No Manager and Bucket available for $container" }

    master.onBind(playable, tag, bucket.manager, container, callback, createNewPlayback@{
      val config = Config(
          tag = options.tag,
          delay = options.delay,
          threshold = options.threshold,
          preload = options.preload,
          repeatMode = options.repeatMode,
          controller = options.controller,
          initialPlaybackInfo = options.initialPlaybackInfo,
          artworkHintListener = options.artworkHintListener,
          tokenUpdateListener = options.tokenUpdateListener,
          networkTypeChangeListener = options.networkTypeChangeListener,
          callbacks = options.callbacks
      )

      return@createNewPlayback when {
        // Scenario: Playable accepts renderer of type PlayerView, and
        // the container is an instance of PlayerView or its subtype.
        playable.config.rendererType.isAssignableFrom(container.javaClass) -> {
          StaticViewRendererPlayback(bucket.manager, bucket, container, config)
        }
        View::class.java.isAssignableFrom(playable.config.rendererType) -> {
          DynamicViewRendererPlayback(bucket.manager, bucket, container, config)
        }
        Fragment::class.java.isAssignableFrom(playable.config.rendererType) -> {
          DynamicFragmentRendererPlayback(bucket.manager, bucket, container, config)
        }
        else -> {
          throw IllegalArgumentException(
              "Unsupported Renderer type: ${playable.config.rendererType}"
          )
        }
      }
    })
    "Request#onBind, $this".logInfo()
  }

  internal fun onRemoved() {
    "Request#onRemoved, $this".logWarn()
    options.controller = null
    options.artworkHintListener = null
    options.networkTypeChangeListener = null
    options.tokenUpdateListener = null
    options.callbacks.clear()
  }

  override fun toString(): String {
    return "Request[t=$tag, c=$container, p=$playable]"
  }
}
