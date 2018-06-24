/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

import android.os.Bundle
import android.view.View
import kohii.media.PlaybackInfo
import kohii.v1.Kohii.Companion.KEY_MANAGER_STATES
import kohii.v1.Kohii.GlobalScrollChangeListener
import kohii.v1.Playback.Token
import java.util.ArrayList
import java.util.Comparator
import java.util.TreeMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
class Manager internal constructor(val kohii: Kohii, val decorView: View) {

  companion object {
    var TOKEN_COMPARATOR: Comparator<Token> = Comparator { o1, o2 -> o1.compareTo(o2) }
  }

  val attachFlag = AtomicBoolean(false)
  val playablesThisActiveTo = ArrayList<Playable>()
  private val maxConcurrentPlayers = 1
  private val scrolling = AtomicBoolean(false)

  val mapTargetToPlayback = WeakHashMap<Any, Playback<*>>()
  val mapPlayableToTarget = HashMap<Playable, Any>()
  @Suppress("MemberVisibilityCanBePrivate")
  val mapPlayableTagToInfo = HashMap<Any, PlaybackInfo>()

  private val mapAttachedPlaybackToTime = HashMap<Playback<*>, Long>()
  private val mapDetachedPlaybackToTime = HashMap<Playback<*>, Long>()

  // Candidates to hold playbacks for a refresh call.
  private val candidates = TreeMap<Token, Playback<*>>(TOKEN_COMPARATOR)

  // Observe the scroll in ViewTreeObserver.
  private var scrollChangeListener: GlobalScrollChangeListener? = null
  private var dispatcher: Dispatcher? = null

  private fun isScrolling() = this.scrolling.get()

  private fun preparePlaybackDestroy(playback: Playback<*>, configChange: Boolean) {
    playback.onPause(configChange)
    playback.onInActive()
    playback.onRemoved(configChange)
  }

  /*
    Called when the Activity bound to this Manager is started.

    [Note, 20180622] If an Activity (say Activity A1) is started by another Activity (say Activity A0),
    the following lifecycle events will be executed:
    A0@onPause() --> A1@onCreate() --> A1@onStart() --> A1@onPostCreate() --> A1@onResume -->
    A1@onPostResume --> A0@onStop --> A0@onSaveInstanceState.
    Therefore, handling Manager activeness of Playable requires some order handling.
   */
  fun onStart() {
    mapAttachedPlaybackToTime.keys.forEach {
      it.playable.mayUpdateStatus(this, true)
      it.onActive()
    }
    this.dispatchRefreshAll()
  }

  // Called when the Activity bound to this Manager is stopped.
  fun onStop(configChange: Boolean) {
    mapAttachedPlaybackToTime.keys.forEach {
      it.onPause(configChange)
      it.playable.mayUpdateStatus(this, configChange)
    }
    // Put it here for future warning.
    // [20180620] Don't call this, as it may change the reason we pause the playback.
    // performRefreshAll();
  }

  fun onSavePlaybackInfo(): Bundle {
    val bundle = Bundle()
    mapTargetToPlayback.values.filter { it.validTag() }.forEach {
      mapPlayableTagToInfo[it.tag] = it.playable.getPlaybackInfo()
    }
    bundle.putSerializable(KEY_MANAGER_STATES, mapPlayableTagToInfo)
    return bundle
  }

  fun onDestroy(configChange: Boolean) {
    mapTargetToPlayback.values.forEach { preparePlaybackDestroy(it, configChange) }
    mapTargetToPlayback.clear()
    mapPlayableToTarget.clear()
  }

  // Get called when the DecorView is attached to Window
  fun onAttached() {
    if (dispatcher == null) dispatcher = Dispatcher(this)
    if (attachFlag.compareAndSet(false, true)) {
      // Do something on the first time this Manager is attached.
      if (this.scrollChangeListener == null) {
        this.scrollChangeListener = GlobalScrollChangeListener(this)
        this.decorView.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
      }

      // Has attached playbacks, and not scrolling, then try refreshing everything.
      if (mapAttachedPlaybackToTime.size > 0 && !isScrolling()) this.dispatchRefreshAll()
    }
  }

  // Get called when the DecorView is detached from Window
  fun onDetached() {
    if (attachFlag.compareAndSet(true, false)) {
      // Do something on the first time this Manager is detached.
      if (this.scrollChangeListener != null) {
        this.decorView.viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
        this.scrollChangeListener = null
      }
    }

    dispatcher?.removeCallbacksAndMessages(null)
    dispatcher = null
  }

  // Once created, the Activity bound to this Manager may have some saved state and want to provide.
  fun onInitialized(cache: Bundle?) {
    if (cache == null) return
    val state = cache.getSerializable(KEY_MANAGER_STATES)
    if (state is HashMap<*, *>) {
      @Suppress("UNCHECKED_CAST")
      mapPlayableTagToInfo.putAll(state as Map<out Any, PlaybackInfo>)
    }
  }

  fun setScrolling(scrolling: Boolean) {
    this.scrolling.set(scrolling)
  }

  fun dispatchRefreshAll() {
    dispatcher?.dispatchRefreshAll()
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  fun <T> onTargetActive(target: T) {
    val now = System.nanoTime()
    val playback = mapTargetToPlayback[target]
    if (playback != null) {
      // TODO [20180620] double check if we should restore state of this Playback or not.
      // restorePlayableState(playback);
      mapAttachedPlaybackToTime[playback] = now
      mapDetachedPlaybackToTime.remove(playback)
      playback.onActive()
      this.dispatchRefreshAll()
    } else {
      throw IllegalStateException("No Playback found for target.")
    }
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  fun <T> onTargetInActive(target: T) {
    val now = System.nanoTime()
    mapTargetToPlayback[target]?.run {
      // TODO [20180620] double check if we should save state of this Playback or not.
      // savePlayableState(playback);
      mapDetachedPlaybackToTime[this] = now
      mapAttachedPlaybackToTime.remove(this)
      this.onPause(false)
      this@Manager.dispatchRefreshAll()
      this.onInActive()
    }
  }

  // Called when something has changed about the Playback. Eg: playback's target has layout change.
  fun <T> onPlaybackInternalChanged(playback: Playback<T>) {
    if (playback.token != null) this.dispatchRefreshAll()
  }

  // Permanently remove the Playback from any cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapTargetToPlayback
  // - mapAttachedPlaybackToTime
  // - mapDetachedPlaybackToTime
  // - mapPlayableTagToInfo
  fun <V> removePlayback(playback: Playback<V>) {
    mapAttachedPlaybackToTime.remove(playback)
    mapDetachedPlaybackToTime.remove(playback)
    mapTargetToPlayback.remove(playback.getTarget())
    preparePlaybackDestroy(playback, false)
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [Manager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  fun <T> addPlayback(playback: Playback<T>): Playback<T> {
    val target = checkNotNull(playback).getTarget()
    var shouldQueue = target != null // playback must have a valid target.
    if (shouldQueue) {
      // Not-null target may be already a target of another Playback before.
      // Here we also make sure if we need to torn that old Playback down first or not.
      val cache = this.mapTargetToPlayback[target]
      shouldQueue = cache == null || cache !== playback
    }

    if (shouldQueue) {
      this.mapTargetToPlayback.put(target, playback)?.run {
        if (mapAttachedPlaybackToTime.remove(this) != null) {
          throw RuntimeException("Old playback is still attached. This should not happen ...")
        } else {
          if (mapDetachedPlaybackToTime.remove(this) != null) {
            // Old playback is in detached cache, we clean its resource.
            this.onInActive()
          }
          this.onRemoved(false)
        }
      }
      playback.playable.mayUpdateStatus(this, true)
      playback.onAdded()
    }

    // In case we are adding nothing new, and the playback is already there.
    if (mapAttachedPlaybackToTime.containsKey(playback)) {
      // shouldQueue is true when the target is not null and no pre-exist playback.
      if (shouldQueue && playback.token != null) this.dispatchRefreshAll()
    }

    return playback
  }

  // Important. Do the refresh stuff. Change the playback items, etc.
  fun performRefreshAll() {
    candidates.clear()
    // List of all possible candidates.
    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    for (playback in playbacks) {
      // Get token here and use later, rather than getting it many times.
      val token = playback.token
      // Doing this will sort the playback using LocToken's center Y value.
      if (token != null) candidates[token] = playback
    }

    val toPlay = LimitedArrayList(candidates.values)
    val count = toPlay.size
    if (maxConcurrentPlayers < count) toPlay.removeRange(maxConcurrentPlayers, count)

    playbacks.removeAll(toPlay)
    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    // Keep only non-play-candidate ones, and pause them.
    for (playback in playbacks) {
      playback.onPause(false)
    }
    // Now kick play the play-candidate
    if (!isScrolling()) {
      for (playback in toPlay) {
        playback.onPlay()
      }
    }
    // Clean up cache
    candidates.clear()
  }
}