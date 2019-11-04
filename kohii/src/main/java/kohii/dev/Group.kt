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

package kohii.dev

import android.os.Handler
import android.os.Message
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.app.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class Group(
  internal val master: Master,
  internal val activity: ComponentActivity
) : LifecycleObserver, Handler.Callback {

  companion object {
    const val DELAY = 2 * 1000L / 60 /* about 2 frames */
    const val MSG_REFRESH = 1
  }

  override fun handleMessage(msg: Message): Boolean {
    if (msg.what == MSG_REFRESH) this.refresh()
    return true
  }

  private val handler = Handler(this)
  private val organizer = Organizer()
  private val dispatcher = PlayableDispatcher()
  private val rendererProviders = mutableMapOf<Class<*>, RendererProvider<*>>()

  internal val managers = mutableSetOf<Manager>()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass !== other?.javaClass) return false
    other as Group
    if (activity !== other.activity) return false
    return true
  }

  override fun hashCode(): Int {
    return activity.hashCode()
  }

  @OnLifecycleEvent(ON_CREATE)
  internal fun onCreate() {
    master.onGroupCreated(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy(owner: LifecycleOwner) {
    handler.removeCallbacksAndMessages(null)
    rendererProviders.onEach { it.value.clear() }
        .clear()
    owner.lifecycle.removeObserver(this)
    master.onGroupDestroyed(this)
  }

  internal fun findHostForContainer(container: ViewGroup): Host? {
    require(ViewCompat.isAttachedToWindow(container))
    return managers.asSequence()
        .mapNotNull { it.findHostForContainer(container) }
        .firstOrNull()
  }

  internal fun <RENDERER : Any> findRendererProvider(playable: Playable<RENDERER>): RendererProvider<RENDERER> {
    val cache = rendererProviders[playable.rendererType]
    @Suppress("UNCHECKED_CAST")
    return requireNotNull(cache) as RendererProvider<RENDERER>
  }

  internal fun <RENDERER : Any> registerRendererProvider(
    type: Class<RENDERER>,
    provider: RendererProvider<RENDERER>
  ) {
    rendererProviders.put(type, provider)
        ?.clear()
  }

  internal fun <RENDERER : Any> unregisterRendererProvider(provider: RendererProvider<RENDERER>) {
    rendererProviders
        .filterValues { it === provider }
        .keys
        .forEach {
          rendererProviders.remove(it)
              ?.clear()
        }
  }

  internal fun <RENDERER : Any> hasRendererProviderForType(type: Class<RENDERER>): Boolean {
    return rendererProviders.containsKey(type)
  }

  internal fun onRefresh() {
    handler.removeMessages(MSG_REFRESH)
    handler.sendEmptyMessageDelayed(MSG_REFRESH, DELAY)
  }

  private fun refresh() {
    val toPlay = linkedSetOf<Playback<*>>()
    val toPause = ArraySet<Playback<*>>()

    managers.forEach {
      val (canPlay, canPause) = it.splitPlaybacks()
      toPlay.addAll(canPlay)
      toPause.addAll(canPause)
    }

    val oldSelection = organizer.selection
    val newSelection = organizer.select(toPlay)

    (toPause + toPlay + oldSelection - newSelection)
        .mapNotNull { playback ->
          master.playables.keys.find { it.playback === playback }
        }
        .forEach { dispatcher.pause(it) }

    newSelection
        .mapNotNull { playback ->
          master.playables.keys.find { it.playback === playback }
        }
        .forEach { dispatcher.play(it) }
  }
}
