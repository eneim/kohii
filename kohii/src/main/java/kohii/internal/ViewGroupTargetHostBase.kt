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

package kohii.internal

import android.view.View
import android.view.ViewGroup
import kohii.v1.Playback
import kohii.v1.PlaybackManager

internal open class ViewGroupTargetHostBase(
  host: ViewGroup,
  manager: PlaybackManager
) : BaseTargetHost<ViewGroup>(host, manager) {

  override fun onAdded() {
    super.onAdded()
    manager.dispatchRefreshAll()
  }

  override fun allowsToPlay(playback: Playback<*>): Boolean {
    return playback.token.shouldPlay()
  }

  override fun accepts(container: Any): Boolean {
    return if (container is View) {
      var view = container
      var parent = view.parent
      while (parent != null && parent !== this.host && parent is View) {
        @Suppress("USELESS_CAST")
        view = parent as View
        parent = view.parent
      }
      parent === this.host
    } else false
  }

  override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
    return super.selectByOrientation(candidates, NONE_AXIS)
  }
}
