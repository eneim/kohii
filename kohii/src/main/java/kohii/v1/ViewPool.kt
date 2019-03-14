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
import androidx.core.util.Pools.SimplePool
import androidx.core.view.contains
import kohii.onEachAcquired

abstract class ViewPool<V : View>(size: Int) : SimplePool<V>(size) {

  abstract fun createView(container: ViewGroup): V

  fun acquireForContainer(
    container: ViewGroup
  ): V {
    val result = super.acquire() ?: this.createView(container)
    // adding result to container may throws exception if the result is added to other
    // container before. client must make sure it remove the result from old container first.
    if (!container.contains(result)) container.addView(result)
    return result
  }

  fun releaseFromContainer(
    container: ViewGroup,
    view: V
  ) {
    if (container.contains(view)) {
      container.removeView(view)
      super.release(view)
    }
  }

  fun cleanUp() {
    this.onEachAcquired { /* ignored */ }
  }
}