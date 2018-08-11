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

import android.view.View
import kohii.media.PlaybackInfo
import kohii.v1.Kohii.GlobalScrollChangeListener
import kohii.v1.Playback.Token
import java.util.Comparator
import java.util.TreeMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
class Manager internal constructor(
    val kohii: Kohii,
    val decorView: View,
    private val playerSelector: PlayerSelector = Manager.DEFAULT_SELECTOR
) {

  class Builder(
      private val kohii: Kohii,
      private val decorView: View,
      private val playerSelector: PlayerSelector = Manager.DEFAULT_SELECTOR
  ) {

    fun build(): Manager {
      return Manager(kohii, decorView, playerSelector)
    }
  }

  companion object {
    val TOKEN_COMPARATOR: Comparator<Token> = Comparator { o1, o2 -> o1.compareTo(o2) }
    val DEFAULT_SELECTOR = object : PlayerSelector {
      override fun select(candidates: List<Playback<*>>): Collection<Playback<*>> {
        return if (candidates.isNotEmpty()) arrayListOf(candidates[0]) else emptyList()
      }
    }
  }

  interface PlayerSelector {
    fun select(candidates: List<Playback<*>>): Collection<Playback<*>>
  }

  private val attachFlag = AtomicBoolean(false)
  private val scrolling = AtomicBoolean(false)  // must start as 'not scrolling'.

  internal val mapWeakPlayableToTarget = WeakHashMap<Playable, Any /* Target */>()
  // TODO [20180806] use WeakHashMap with ReferenceQueue and catch the QC-ed Target in cleanup thread?
  internal val mapTargetToPlayback = HashMap<Any? /* Target */, Playback<*>>()

  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()
  private val mapAttachedPlaybackToTime = HashMap<Playback<*>, Long>()
  private val mapDetachedPlaybackToTime = HashMap<Playback<*>, Long>()

  // Candidates to hold playbacks for a refresh call.
  private val candidates = TreeMap<Token, Playback<*>>(TOKEN_COMPARATOR)

  // Observe the scroll in ViewTreeObserver.
  private var scrollChangeListener: GlobalScrollChangeListener? = null
  // Dispatch certain events/actions
  private var dispatcher: Dispatcher? = null

  private fun isScrolling() = this.scrolling.get()

  /*
    Called when the Activity bound to this Manager is started.

    [Note, 20180622]
    If an Activity (say Activity A1) is started by another Activity (say Activity A0), the following
    lifecycle events will be executed:
    A0@onPause() --> A1@onCreate() --> A1@onStart() --> A1@onPostCreate() --> A1@onResume() -->
    A1@onPostResume() --> A0@onStop() --> A0@onSaveInstanceState().
    Therefore, handling Manager activeness of Playable requires some order handling.
   */
  internal fun onHostStarted() {
    mapAttachedPlaybackToTime.keys.forEach {
      kohii.onManagerActiveForPlayback(this, it)
    }
    this.dispatchRefreshAll()
  }

  // Called when the Activity bound to this Manager is stopped.
  internal fun onHostStopped(configChange: Boolean) {
    mapAttachedPlaybackToTime.keys.forEach {
      kohii.onManagerInActiveForPlayback(this, it, configChange)
    }
  }

  // Called when the Activity bound to this Manager is destroyed.
  internal fun onHostDestroyed() {
    ArrayList(mapTargetToPlayback.values).apply {
      this.forEach { performDestroyPlayback(it) }
    }.clear()
    mapWeakPlayableToTarget.apply {
      this.entries.forEach {
        if (kohii.mapWeakPlayableToManager[it.key] == this@Manager) {
          kohii.mapWeakPlayableToManager[it.key] = null
        }
      }
    }.clear()
  }

  // Get called when the DecorView is attached to Window
  internal fun onAttached() {
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
  internal fun onDetached() {
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

  internal fun savePlaybackInfo(playback: Playback<*>) {
    if (playback.validTag()) {
      mapPlayableTagToInfo[playback.tag] = playback.playable.playbackInfo
    }
  }

  internal fun restorePlaybackInfo(playback: Playback<*>) {
    if (playback.validTag()) {
      val info = mapPlayableTagToInfo.remove(playback.tag)
      if (info != null) playback.playable.playbackInfo = info
    }
  }

  internal fun setScrolling(scrolling: Boolean) {
    this.scrolling.set(scrolling)
  }

  internal fun dispatchRefreshAll() {
    dispatcher?.dispatchRefreshAll() ?: performRefreshAll()
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  fun onTargetAvailable(target: Any) {
    mapTargetToPlayback[target]?.run {
      mapAttachedPlaybackToTime[this] = System.nanoTime()
      mapDetachedPlaybackToTime.remove(this)
      if (this@Manager.mapWeakPlayableToTarget[this.playable] == this.target) {
        restorePlaybackInfo(this)
        this.prepare()
      }
      this.onTargetAvailable()
      this@Manager.dispatchRefreshAll()
    } ?: throw IllegalStateException("No Playback found for target: $target")
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  fun onTargetUnAvailable(target: Any) {
    mapTargetToPlayback[target]?.run {
      mapDetachedPlaybackToTime[this] = System.nanoTime()
      mapAttachedPlaybackToTime.remove(this)
      this.pause()
      this@Manager.dispatchRefreshAll()
      this.onTargetUnAvailable()
      if (this@Manager.mapWeakPlayableToTarget[this.playable] == this.target) {
        savePlaybackInfo(this)
        this.release()
      }
    }
  }

  // Called when something has changed about the Playback. Eg: playback's target has layout change.
  fun <T> onPlaybackInternalChanged(playback: Playback<T>) {
    if (!isScrolling()) {
      if (playback.token?.wantsToPlay() == true) this.dispatchRefreshAll()
    }
  }

  // Permanently remove the Playback from any cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapTargetToPlayback
  // - mapAttachedPlaybackToTime
  // - mapDetachedPlaybackToTime
  // - mapPlayableTagToInfo
  fun performDestroyPlayback(playback: Playback<*>) {
    mapAttachedPlaybackToTime.remove(playback)
    mapDetachedPlaybackToTime.remove(playback)
    mapTargetToPlayback.remove(playback.target).also { it?.onTargetUnAvailable() }
    playback.onRemoved()
    playback.onDestroyed()
  }

  /**
   * Once [Playable.bind] is called, it will create a new [Playback] object
   * to manage the Target. [Manager] will then add that [Playback] to cache for management.
   * Old [Playback] will be cleaned up and removed.
   */
  fun <T> performAddPlayback(playback: Playback<T>): Playback<T> {
    val target = playback.target
    var shouldAdd = target != null // playback must have a valid target.
    if (shouldAdd) {
      // Not-null target may be already a target of another Playback before.
      // Here we also make sure if we need to torn that old Playback down first or not.
      val cache = this.mapTargetToPlayback[target]
      shouldAdd = cache == null || cache !== playback
    }

    if (shouldAdd) {
      this.mapTargetToPlayback.put(target!!, playback)?.run {
        if (mapAttachedPlaybackToTime.remove(this) != null) {
          throw RuntimeException("Old playback is still attached. This should not happen ...")
        } else {
          mapDetachedPlaybackToTime.remove(this)
        }
        this.onRemoved()
      }
      playback.onAdded()
    }

    // In case we are adding nothing new, and the playback is already there.
    if (mapAttachedPlaybackToTime.containsKey(playback)) {
      // shouldQueue is true when the target is not null and no pre-exist playback.
      if (shouldAdd && playback.token?.wantsToPlay() == true) this.dispatchRefreshAll()
    }

    return playback
  }

  // Important. Do the refresh stuff. Change the playback items, etc.
  fun performRefreshAll() {
    candidates.clear()

    mapDetachedPlaybackToTime.keys.filter { it.token != null }.also { it ->
      dispatcher?.apply {
        it.forEach { this.dispatchTargetAvailable(it) }
      }
    }

    // List of all possible candidates.
    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    for (playback in playbacks) {
      // Get token here and use later, rather than getting it many times.
      val token = playback.token
      // Doing this will sort the playback using LocToken's center Y value.
      if (token == null) {
        dispatcher?.dispatchTargetUnAvailable(playback)
      } else if (token.wantsToPlay()) candidates[token] = playback
    }

    val toPlay = playerSelector.select(ArrayList(candidates.values))

    playbacks.removeAll(toPlay)
    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    // Keep only non-play-candidate ones, and pause them.
    for (playback in playbacks) {
      playback.pause()
    }
    // Now kick play the play-candidate
    if (!isScrolling()) {
      for (playback in toPlay) {
        playback.play()
      }
    }
    // Clean up cache
    candidates.clear()
  }
}