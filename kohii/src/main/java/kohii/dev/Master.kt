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

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ComponentActivity
import androidx.core.net.toUri
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.Media
import kohii.media.MediaItem
import kohii.media.PlaybackInfo
import kohii.v1.Kohii
import java.lang.ref.WeakReference
import kotlin.LazyThreadSafetyMode.NONE

class Master(context: Context) : PlayableManager {

  companion object {

    private const val MSG_STARTUP = 100

    internal val NO_TAG = Any()

    @Volatile private var master: Master? = null

    @JvmStatic
    operator fun get(context: Context) = master ?: synchronized(Master::class.java) {
      master ?: Master(context).also { master = it }
    }

    @JvmStatic
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  val app = context.applicationContext as Application
  val kohii = Kohii[app]

  private val groups = mutableSetOf<Group>()
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()
  internal val playables = mutableMapOf<Playable<*>, Any /* tag */>()

  private fun registerInternal(
    activity: ComponentActivity,
    host: Any,
    managerLifecycleOwner: LifecycleOwner,
    vararg views: View
  ): Manager {
    val group = groups.find { it.activity === activity } ?: Group(this, activity).also {
      activity.lifecycle.addObserver(it)
    }

    val manager = group.managers.find { it.lifecycleOwner === managerLifecycleOwner }
        ?: Manager(group, host, managerLifecycleOwner)

    views.forEach { manager.registerHost(it) }

    manager.lifecycleOwner.lifecycle.addObserver(manager)
    return manager
  }

  internal fun <CONTAINER : ViewGroup, RENDERER : Any> bind(
    playable: Playable<RENDERER>,
    container: CONTAINER,
    callback: ((Playback<*>) -> Unit)? = null
  ) {
    // 1. Find the Manager for the container
    val weakCallback = WeakReference(callback)
    container.doOnAttach {
      val host = groups.asSequence()
          .mapNotNull { it.findHostForContainer(container) }
          .firstOrNull()

      requireNotNull(host) { "No Manager and Host available for $container" }

      val createNew by lazy(NONE) {
        if (host.manager.group.hasRendererProviderForType(container.javaClass))
          Playback(host.manager, host, container)
        else
          LazyPlayback(host.manager, host, container)
      }

      val sameContainer = host.manager.playbacks[container]
      val samePlayable = host.manager.playbacks.asSequence()
          .firstOrNull { playable.playback === it.value }
          ?.value

      val nextPlayback =
        if (sameContainer == null) { // Bind to new Container
          if (samePlayable == null) {
            // both sameContainer and samePlayable are null --> fresh binding
            playable.playback = createNew
            host.manager.addPlayback(createNew)
            createNew
          } else {
            // samePlayable is not null --> a bound Playable to be rebound to other/new Container
            // Action: create new Playback for new Container, make the new binding and remove old binding of
            // the 'samePlayable' Playback
            playable.playback = createNew
            samePlayable.manager.removePlayback(samePlayable)
            host.manager.addPlayback(createNew)
            createNew
          }
        } else {
          if (samePlayable == null) {
            // sameContainer is not null but samePlayable is null --> new Playable is bound to a bound Container
            // Action: create new Playback for current Container, make the new binding and remove old binding of
            // the 'sameContainer'
            playable.playback = createNew
            sameContainer.manager.removePlayback(sameContainer)
            host.manager.addPlayback(createNew)
            createNew
          } else {
            // both sameContainer and samePlayable are not null --> a bound Playable to be rebound to a bound Container
            if (sameContainer === samePlayable) {
              // Nothing to do
              samePlayable
            } else {
              // Scenario: rebind a bound Playable from one Container to other Container that is being bound.
              // Action: remove both 'sameContainer' and 'samePlayable', create new one for the Container.
              // to the Container
              playable.playback = createNew
              sameContainer.manager.removePlayback(sameContainer)
              samePlayable.manager.removePlayback(samePlayable)
              host.manager.addPlayback(createNew)
              createNew
            }
          }
        }

      weakCallback.get()
          ?.invoke(nextPlayback)
    }
  }

  private val defaultPlayableProvider by lazy(NONE) {
    val playableCreator = object : PlayableCreator<PlayerView> {
      override fun createPlayable(
        master: Master,
        config: Playable.Config,
        media: Media
      ): Playable<PlayerView> {
        return Playable(
            master,
            media,
            config,
            PlayerView::class.java,
            master.kohii.defaultBridgeProvider.provideBridge(master.kohii, media)
        )
      }
    }
    PlayerViewPlayableProvider(this, playableCreator)
  }

  internal fun tearDown(playable: Playable<*>) {
    check(playable.manager == null)
    check(playable.playback == null)
    playable.onPause()
    playable.onRelease()
    playables.remove(playable)
    playbackInfoStore.remove(playable.tag)
  }

  internal fun trySavePlaybackInfo(playable: Playable<*>) {
    if (playable.tag === NO_TAG) return
    if (!playbackInfoStore.containsKey(playable.tag)) {
      playbackInfoStore[playable.tag] = playable.playbackInfo
    }
  }

  internal fun tryRestorePlaybackInfo(playable: Playable<*>) {
    if (playable.tag === NO_TAG) return
    val cache = playbackInfoStore.remove(playable.tag)
    if (cache != null) playable.playbackInfo = cache
  }

  private fun checkPlayables() {
    playables.filter { it.key.manager === this }
        .keys.toMutableList()
        .onEach {
          it.manager = null
          tearDown(it)
        }
        .clear()
  }

  private val dispatcher = Dispatcher(this)

  internal fun onGroupCreated(group: Group) {
    groups.add(group)
    dispatcher.sendEmptyMessage(MSG_STARTUP)
  }

  internal fun onGroupDestroyed(group: Group) {
    groups.remove(group)
    if (groups.isEmpty()) dispatcher.removeCallbacksAndMessages(null)
  }

  private class Dispatcher(val master: Master) : Handler(Looper.getMainLooper()) {

    override fun handleMessage(msg: Message) {
      if (msg.what == MSG_STARTUP) {
        master.checkPlayables()
      }
    }
  }

  // Public APIs

  fun register(
    fragment: Fragment,
    vararg views: View
  ): Manager {
    val (activity, managerLifecycleOwner) = fragment.requireActivity() to fragment.viewLifecycleOwner
    return registerInternal(activity, fragment, managerLifecycleOwner, *views)
  }

  fun register(
    activity: ComponentActivity,
    vararg views: View
  ): Manager {
    return registerInternal(activity, activity, activity, *views)
  }

  fun setUp(media: Media): Binder<PlayerView> {
    return Binder(this, media, defaultPlayableProvider)
  }

  fun setUp(uri: Uri) = setUp(MediaItem(uri))

  fun setUp(url: String) = setUp(url.toUri())
}
