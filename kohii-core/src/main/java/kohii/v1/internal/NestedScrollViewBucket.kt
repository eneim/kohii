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
import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import kohii.v1.core.Bucket
import kohii.v1.core.Manager
import kohii.v1.core.Playback
import kohii.v1.core.Selector
import kohii.v1.core.Strategy

internal class NestedScrollViewBucket(
  manager: Manager,
  override val root: NestedScrollView,
  strategy: Strategy,
  selector: Selector
) : Bucket(manager, root, strategy, selector), OnScrollChangeListener {

  override fun onScrollChange(
    v: NestedScrollView?,
    scrollX: Int,
    scrollY: Int,
    oldScrollX: Int,
    oldScrollY: Int
  ) {
    manager.refresh()
  }

  override fun onAdded() {
    super.onAdded()
    root.setOnScrollChangeListener(this)
  }

  override fun onRemoved() {
    super.onRemoved()
    root.setOnScrollChangeListener(null as OnScrollChangeListener?)
  }

  override fun accepts(container: ViewGroup): Boolean {
    var view = container as View
    var parent = view.parent
    while (parent != null && parent !== this.root && parent is View) {
      view = parent
      parent = view.parent
    }
    return parent === this.root
  }

  override fun selectToPlay(candidates: Collection<Playback>): Collection<Playback> {
    return selectByOrientation(candidates, orientation = VERTICAL)
  }
}
