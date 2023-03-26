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
import android.view.View
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
import kohii.v1.core.Scope.BUCKET
import kohii.v1.core.Scope.GROUP
import kohii.v1.core.Scope.MANAGER
import kohii.v1.core.Scope.PLAYBACK
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

  /**
   * @see Manager.applyVolumeInfo
   */
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

  /**
   * Locks all the Playbacks of a [FragmentActivity]. The locking [Scope] is equal to [Scope.GROUP].
   * Any call to unlock of smaller [Scope] (like [Scope.MANAGER]) will not unlock the Playbacks.
   *
   * @see [Master.lock]
   */
  fun lockActivity(activity: FragmentActivity) {
    master.lock(activity, GROUP)
  }

  /**
   * Unlock all the Playbacks of an [FragmentActivity]. The effective scope is [Scope.GROUP]. If it
   * was locked by a call to higher [Scope] like [Scope.GLOBAL], this method does nothing. Once
   * unlocked, it will unlock all locked objects within [Scope.GROUP]: [Playback], [Bucket],
   * [Manager].
   */
  fun unlockActivity(activity: FragmentActivity) {
    master.unlock(activity, GROUP)
  }

  /**
   * Locks all the Playbacks of a [Manager]. The locking [Scope] is equal to [Scope.MANAGER].
   * Any call to unlock of smaller [Scope] (like [Scope.BUCKET]) will not unlock the Playbacks.
   *
   * @see [Master.lock]
   */
  fun lockManager(manager: Manager) {
    master.lock(manager, MANAGER)
  }

  /**
   * Unlock all the Playbacks of a [Manager]. The effective scope is [Scope.MANAGER]. If it was
   * locked by a call to higher [Scope] like [Scope.GROUP], this method does nothing. Once unlocked,
   * it will unlock all locked objects within [Scope.MANAGER]: [Playback], [Bucket].
   */
  fun unlockManager(manager: Manager) {
    master.unlock(manager, MANAGER)
  }

  /**
   * Locks all the Playbacks of a [Bucket] whose [Bucket.root] is [view]. The locking [Scope] is
   * equal to [Scope.BUCKET]. Any call to unlock of smaller [Scope] (like [Scope.PLAYBACK]) will
   * not unlock the Playbacks.
   *
   * @see [Master.lock]
   */
  fun lockBucket(view: View) {
    master.lock(view, BUCKET)
  }

  /**
   * Unlock all the Playbacks of a [Bucket] whose [Bucket.root] is [view]. The effective scope is
   * [Scope.BUCKET]. If it was locked by a call to higher [Scope] like [Scope.MANAGER], this method
   * does nothing. Once unlocked, it will unlock all locked objects within [Scope.BUCKET]:
   * [Playback].
   */
  fun unlockBucket(view: View) {
    master.unlock(view, BUCKET)
  }

  /**
   * Locks all the Playbacks of a [Playback]. The locking [Scope] is equal to [Scope.PLAYBACK].
   */
  fun lockPlayback(playback: Playback) {
    master.lock(playback, PLAYBACK)
  }

  /**
   * Unlock all the Playbacks of a [Playback]. The effective scope is [Scope.PLAYBACK]. If it was
   * locked by a call to higher [Scope] like [Scope.BUCKET], this method does nothing.
   */
  fun unlockPlayback(playback: Playback) {
    master.unlock(playback, PLAYBACK)
  }

  @CallSuper
  open fun cleanUp() {
    playableCreator.cleanUp()
  }
}
