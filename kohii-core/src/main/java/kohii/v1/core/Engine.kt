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

import android.net.Uri
import androidx.annotation.CallSuper
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import kohii.v1.core.Binder.Options
import kohii.v1.core.Master.MemoryMode
import kohii.v1.core.Master.MemoryMode.LOW
import kohii.v1.media.Media
import kohii.v1.media.MediaItem
import kohii.v1.media.VolumeInfo

// TODO may need RendererProvider<RENDERER> in Engine instance? Problem: we want to keep Engine
//  instance at global/Application scope. Since Renderer must live in Activity scope, keeping a
//  RendererProvider in Engine may cause a memory leak if not well designed.

// TODO support manual Playback creation in Engine instance.
// TODO need to support sub-type of a Renderer type. Eg: PlayerView and classes that extend it.
abstract class Engine<RENDERER : Any>(
  val master: Master,
  internal val creator: Creator
) {

  // TODO implement the method below.
  // abstract fun <T> supportRendererType(type: Class<T>): Boolean

  inline fun setUp(
    media: Media,
    crossinline options: Options.() -> Unit = {}
  ): Binder<RENDERER> = Binder(
      this, media
  ).also { options(it.options) }

  inline fun setUp(
    uri: Uri,
    crossinline options: Options.() -> Unit = {}
  ) = setUp(MediaItem(uri), options)

  inline fun setUp(
    url: String,
    crossinline options: Options.() -> Unit = {}
  ) = setUp(url.toUri(), options)

  fun fetchRebinder(tag: Any?): Rebinder? {
    return if (tag == null) null else Rebinder(tag)
  }

  fun register(
    fragment: Fragment,
    memoryMode: MemoryMode = LOW
  ): Manager = master.register(fragment, memoryMode)

  fun register(
    activity: FragmentActivity,
    memoryMode: MemoryMode = LOW
  ): Manager = master.register(activity, memoryMode)

  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    target: Any,
    scope: Scope
  ) {
    when (target) {
      is Playback -> target.manager.applyVolumeInfo(volumeInfo, target, scope)
      is Host -> target.manager.applyVolumeInfo(volumeInfo, target, scope)
      is Manager -> target.applyVolumeInfo(volumeInfo, target, scope)
      is Group -> target.managers.forEach { it.applyVolumeInfo(volumeInfo, it, scope) }
      else -> throw IllegalArgumentException("Unknown target for VolumeInfo: $target")
    }
  }

  fun stick(playback: Playback) {
    playback.manager.stick(playback.host)
    playback.manager.group.stick(playback.manager)
    playback.manager.refresh()
  }

  fun stick(lifecycleOwner: LifecycleOwner) {
    val manager = master.groups.asSequence()
        .map { it.managers.find { m -> m.lifecycleOwner === lifecycleOwner } }
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

  fun unstick(playback: Playback) {
    playback.manager.group.unstick(playback.manager)
    playback.manager.unstick(playback.host)
    playback.manager.refresh()
  }

  @CallSuper
  open fun cleanUp() {
    creator.cleanUp()
  }
}
