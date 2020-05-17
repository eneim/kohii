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

import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains
import kohii.v1.core.Bucket
import kohii.v1.core.Manager
import kohii.v1.core.Playback

// Playback whose Container is to contain the actual Renderer, and the Renderer is created
// on-demand, right before the playback should start.
internal class DynamicViewRendererPlayback(
  manager: Manager,
  bucket: Bucket,
  container: ViewGroup,
  config: Config
) : Playback(manager, bucket, container, config) {

  override fun onPlay() {
    playable?.setupRenderer(this)
    super.onPlay()
  }

  override fun onPause() {
    super.onPause()
    playable?.teardownRenderer(this)
  }

  override fun onAttachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is View && renderer !== container)
    if (container.contains(renderer)) return false

    val parent = renderer.parent
    if (parent is ViewGroup && parent !== container) {
      parent.removeView(renderer)
    }

    // default implementation
    container.removeAllViews()
    container.addView(renderer)
    return true
  }

  override fun onDetachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is View && renderer !== container)
    if (!container.contains(renderer)) return false
    container.removeView(renderer)
    return true
  }
}
