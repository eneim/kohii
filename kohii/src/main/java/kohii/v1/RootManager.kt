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

import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * Bind to an Activity, to manage [PlaybackManager]s inside.
 *
 * This is to support many [PlaybackManager]s in one Activity. For example: ViewPager whose pages
 * are Fragments, List/Detail UI, etc. When a [PlaybackManager] dispatch the refresh event, it will
 * call this class's [dispatchManagerRefresh] so that all other [PlaybackManager] in the same host
 * will also be notified.
 */
class RootManager(
  val kohii: Kohii,
  lifecycleOwner: LifecycleOwner /* the Activity */
) : LifecycleObserver {

  init {
    lifecycleOwner.lifecycle.addObserver(this)
  }

  private val managers = HashMap<LifecycleOwner, PlaybackManager>()

  internal fun attachPlaybackManager(
    lifecycleOwner: LifecycleOwner,
    playbackManager: PlaybackManager
  ) {
    if (!this.managers.containsKey(lifecycleOwner)) {
      this.managers[lifecycleOwner] = playbackManager
    }
  }

  internal fun detachPlaybackManager(playbackManager: PlaybackManager) {
    val cache = this.managers.filterValues { it === playbackManager }
    for (item in cache) this.managers.remove(item.key)
  }

  fun dispatchManagerRefresh(): Boolean {
    this.managers.forEach { it.value.dispatchRefreshAllInternal() }
    return true
  }

  @OnLifecycleEvent(ON_DESTROY)
  fun onOwnerDestroy(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.removeObserver(this)
    kohii.parents.remove(lifecycleOwner)
  }
}