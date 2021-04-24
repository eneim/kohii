/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kohii.v1.core.Bridge
import kohii.v1.core.Common
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.Playable
import kohii.v1.core.Playback
import kohii.v1.debugOnly
import kohii.v1.logDebug
import kohii.v1.logInfo
import kohii.v1.media.PlaybackInfo

/**
 * This [ViewModel] is used to manage the [PlaybackInfo] cache for the [Playback]s the [Manager] is
 * managing. It should also be aware of the [Playable]s being used by them.
 *
 * When the [Manager] is active (its [Manager.lifecycleOwner] is at least
 * [Manager.activeLifecycleState]):
 * - If a [Playback] is detached, the [PlaybackInfo] of its [Playable] should be saved to this class.
 * - If a [Playback] is (re)attached, the [PlaybackInfo] of its [Playable] should be restored from
 * this class, and that information is also removed from the cache.
 * - If a [Playback] from another [Manager] takes away the [Playable] from a [Playback] managed by
 * the [Manager] that owns this class, it should transfer any cached [PlaybackInfo] of that
 * [Playable] from this class to the [ManagerViewModel] of that [Manager].
 */
internal class ManagerViewModel(application: Application) : AndroidViewModel(application) {

  /**
   * The [Master].
   */
  private val master = Master[application]

  /**
   * A set of [Playable]s those are currently managed by the [Manager] owning this [ViewModel]
   */
  private val playables = mutableSetOf<Playable>()

  /**
   * [PlaybackInfo] cache for the [Playable]s managed by this class.
   */
  private val playbackInfoStore = mutableMapOf<Any /* Playable tag */, PlaybackInfo>()

  init {
    master.onViewModelCreated(this@ManagerViewModel)
  }

  internal fun addPlayable(playable: Playable) {
    playables.add(playable)
  }

  internal fun removePlayable(playable: Playable) {
    if (playables.remove(playable)) {
      val infoKey = if (playable.tag !== Master.NO_TAG) {
        playable.tag
      } else {
        // if there is no available tag, we use the playback as info key, to allow state caching in
        // the same session.
        val playback = playable.playback
        if (playback == null || !playback.isAttached) return
        playback
      }
      playbackInfoStore.remove(infoKey)
    }
  }

  internal fun movePlayable(playable: Playable, destination: ManagerViewModel) {
    if (playables.contains(playable)) {
      tryRestorePlaybackInfo(playable)
      removePlayable(playable)
      destination.addPlayable(playable)
      destination.trySavePlaybackInfo(playable)
    }
  }

  /**
   * @see [Manager.trySavePlaybackInfo]
   */
  internal fun trySavePlaybackInfo(playable: Playable) {
    if (!playables.contains(playable)) {
      debugOnly {
        error("Playable $playable is not added to this ViewModel.")
      }
      return
    }

    "ViewModel#trySavePlaybackInfo: $playable".logDebug()
    val key = if (playable.tag !== Master.NO_TAG) {
      playable.tag
    } else {
      // If there is no available tag, we use the playback as info key, to allow state caching in
      // the same session.
      val playback = playable.playback
      if (playback == null || !playback.isAttached) return
      playback
    }

    if (!playbackInfoStore.containsKey(key)) {
      val info = playable.playbackInfo
      "ViewModel#trySavePlaybackInfo: $info, $playable".logInfo()
      playbackInfoStore[key] = info
    }
  }

  /**
   * Note: If this method is called, it must be before any call to [Bridge.prepare].
   *
   * @see [Manager.tryRestorePlaybackInfo]
   */
  internal fun tryRestorePlaybackInfo(playable: Playable) {
    if (!playables.contains(playable)) {
      debugOnly {
        error("Playable $playable is not added to this ViewModel.")
      }
      return
    }

    "ViewModel#tryRestorePlaybackInfo: $playable".logDebug()
    val cache = if (playable.tag !== Master.NO_TAG) {
      playbackInfoStore.remove(playable.tag)
    } else {
      val key = playable.playback ?: return
      playbackInfoStore.remove(key)
    }

    "ViewModel#tryRestorePlaybackInfo: $cache, $playable".logInfo()
    // Only restoring playback state if there is cached state, and the player is not ready yet.
    if (cache != null && playable.playerState <= Common.STATE_IDLE) {
      playable.playbackInfo = cache
    }
  }

  override fun onCleared() {
    playbackInfoStore.clear()
    master.onViewModelCleared(this)
  }
}
