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

import android.view.ViewGroup
import kohii.v1.core.Bucket
import kohii.v1.core.Manager
import kohii.v1.core.Playback

// Playback whose Container is also the Renderer.
// This Playback will request the Playable to setup the Renderer as soon as it is active, and
// release the Renderer as soon as it is inactive.
internal class StaticViewRendererPlayback(
  manager: Manager,
  bucket: Bucket,
  container: ViewGroup,
  config: Config
) : Playback(manager, bucket, container, config) {

  override fun onActive() {
    super.onActive()
    playable?.setupRenderer(this)
  }

  override fun onInActive() {
    playable?.teardownRenderer(this)
    super.onInActive()
  }

  override fun acquireRenderer(): Any? {
    return this.container
  }

  /**
   * This operation would always be false if the renderer is not null, since the renderer is never
   * released to any pool.
   */
  override fun releaseRenderer(renderer: Any?) = renderer == null

  override fun onAttachRenderer(renderer: Any?): Boolean {
    require(renderer == null || renderer === container)
    return true // true because we can always use this renderer.
  }

  override fun onDetachRenderer(renderer: Any?): Boolean {
    require(renderer == null || renderer === container)
    return true
  }
}
