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

import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains

open class ViewTarget<CONTAINER : ViewGroup, RENDERER : View>(override val container: CONTAINER) :
    Target<CONTAINER, RENDERER> {

  override fun attachRenderer(renderer: RENDERER) {
    if (container === renderer || container::javaClass === renderer::javaClass) return
    if (!container.contains(renderer)) container.addView(renderer)
  }

  override fun detachRenderer(renderer: RENDERER): Boolean {
    if (container === renderer || container::javaClass === renderer::javaClass) return false
    if (!container.contains(renderer)) return false
    container.removeView(renderer)
    return true
  }
}
