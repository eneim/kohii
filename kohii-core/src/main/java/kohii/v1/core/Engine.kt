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

package kohii.v1.core

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleOwner
import kohii.v1.core.Binder.Options
import kohii.v1.core.MemoryMode.LOW
import kohii.v1.media.Media
import kohii.v1.media.MediaItem
import kohii.v1.media.VolumeInfo

abstract class Engine<RENDERER : Any> constructor(
  val master: Master,
  val playableCreator: PlayableCreator<RENDERER>
) {

  constructor(
    context: Context,
    playableCreator: PlayableCreator<RENDERER>
  ) : this(Master[context], playableCreator)

  internal fun inject(group: Group) {
    group.managers.forEach { prepare(it) }
  }

  abstract fun prepare(manager: Manager)

  @JvmOverloads
  inline fun setUp(
    media: Media,
    crossinline options: Options.() -> Unit = {}
  ): Binder = Binder(this, media)
      .also { options(it.options) }

  @JvmOverloads
  inline fun setUp(
    uri: Uri,
    crossinline options: Options.() -> Unit = {}
  ) = setUp(MediaItem(uri), options)

  @JvmOverloads
  inline fun setUp(
    url: String,
    crossinline options: Options.() -> Unit = {}
  ) = setUp(url.toUri(), options)

  fun cancel(tag: Any) {
    master.requests.filterValues { it.tag == tag }
        .forEach {
          it.value.playable.playback = null
          master.requests.remove(it.key)?.onRemoved()
        }
  }

  fun cancel(container: ViewGroup) {
    master.removeBinding(container)
  }

  @Deprecated(
      "Just create the Rebinder directly.",
      ReplaceWith("Rebinder(tag)", "kohii.v1.core.Rebinder")
  )
  fun fetchRebinder(tag: Any?): Rebinder? {
    return if (tag == null) null else Rebinder(tag)
  }

  @JvmOverloads
  fun register(
    fragment: Fragment,
    memoryMode: MemoryMode = LOW,
    activeLifecycleState: State = STARTED
  ): Manager {
    val (activity, lifecycleOwner) = fragment.requireActivity() to fragment.viewLifecycleOwner
    return master.registerInternal(
        activity = activity,
        host = fragment,
        managerLifecycleOwner = lifecycleOwner,
        memoryMode = memoryMode,
        activeLifecycleState = activeLifecycleState
    )
  }

  @JvmOverloads
  fun register(
    activity: FragmentActivity,
    memoryMode: MemoryMode = LOW,
    activeLifecycleState: State = STARTED
  ): Manager = master.registerInternal(
      activity = activity,
      host = activity,
      managerLifecycleOwner = activity,
      memoryMode = memoryMode,
      activeLifecycleState = activeLifecycleState
  )

  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    target: Any,
    scope: Scope
  ) {
    when (target) {
      is Playback -> target.manager.applyVolumeInfo(volumeInfo, target, scope)
      is Bucket -> target.manager.applyVolumeInfo(volumeInfo, target, scope)
      is Manager -> target.applyVolumeInfo(volumeInfo, target, scope)
      is Group -> target.managers.forEach { it.applyVolumeInfo(volumeInfo, it, scope) }
      else -> throw IllegalArgumentException("Unknown target for VolumeInfo: $target")
    }
  }

  fun stick(playback: Playback) {
    playback.manager.stick(playback.bucket)
    playback.manager.group.stick(playback.manager)
    playback.manager.refresh()
  }

  fun unstick(playback: Playback) {
    playback.manager.group.unstick(playback.manager)
    playback.manager.unstick(playback.bucket)
    playback.manager.refresh()
  }

  fun stick(lifecycleOwner: LifecycleOwner) {
    val manager = master.groups.asSequence()
        .map {
          it.managers.find { m -> m.lifecycleOwner === lifecycleOwner }
        }
        .firstOrNull()
    if (manager != null) {
      manager.group.stick(manager)
      manager.refresh()
    }
  }

  fun unstick(lifecycleOwner: LifecycleOwner) {
    val manager = master.groups.asSequence()
        .map { it.managers.find { m -> m.lifecycleOwner === lifecycleOwner } }
        .firstOrNull()
    if (manager != null) {
      manager.group.unstick(manager)
      manager.refresh()
    }
  }

  @CallSuper
  open fun cleanUp() {
    playableCreator.cleanUp()
  }
}
