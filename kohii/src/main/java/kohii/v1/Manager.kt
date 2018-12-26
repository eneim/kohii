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

import android.graphics.Rect
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
      override fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>> {
        return if (candidates.isNotEmpty()) arrayListOf(candidates.first()) else emptyList()
      }
    }
  }

  interface PlayerSelector {
    fun select(candidates: Collection<Playback<*>>): Collection<Playback<*>>
  }

  private val attachFlag = AtomicBoolean(false)
  private val scrolling = AtomicBoolean(false)  // must start as 'not scrolling'.

  internal val viewRect: Rect by lazy { Rect().also { this.decorView.getLocalVisibleRect(it) } }

  internal val mapWeakPlayableToTarget = WeakHashMap<Playable, Any /* Target */>()
  // TODO [20180806] use WeakHashMap with ReferenceQueue and catch the QC-ed Target in cleanup thread?
  internal val mapTargetToPlayback = LinkedHashMap<Any? /* Target */, Playback<*>>()

  @Suppress("MemberVisibilityCanBePrivate")
  internal val mapAttachedPlaybackToTime = LinkedHashMap<Playback<*>, Long>()
  @Suppress("MemberVisibilityCanBePrivate")
  internal val mapDetachedPlaybackToTime = LinkedHashMap<Playback<*>, Long>()

  private val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()

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
    mapAttachedPlaybackToTime.forEach {
      kohii.onManagerActiveForPlayback(this, it.key)
    }
    this.dispatchRefreshAll()
  }

  // Called when the Activity bound to this Manager is stopped.
  internal fun onHostStopped(configChange: Boolean) {
    mapAttachedPlaybackToTime.forEach {
      kohii.onManagerInActiveForPlayback(this, it.key, configChange)
    }
  }

  // Called when the Activity bound to this Manager is destroyed.
  internal fun onHostDestroyed() {
    // Wrap by an ArrayList because we also remove entry while iterating by performDestroyPlayback
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
    if (playback.playable.tag != Playable.NO_TAG) {
      mapPlayableTagToInfo[playback.playable.tag] = playback.playable.playbackInfo
    }
  }

  internal fun restorePlaybackInfo(playback: Playback<*>) {
    if (playback.playable.tag != Playable.NO_TAG) {
      val info = mapPlayableTagToInfo.remove(playback.playable.tag)
      if (info != null) playback.playable.playbackInfo = info
    }
  }

  internal fun setScrolling(scrolling: Boolean) {
    this.scrolling.set(scrolling)
  }

  internal fun dispatchRefreshAll() {
    dispatcher?.dispatchRefreshAll()
  }

  // Called when a Playback's target is attached. Eg: PlayerView is attached to window.
  fun onTargetAvailable(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapAttachedPlaybackToTime[it] = System.nanoTime()
      mapDetachedPlaybackToTime.remove(it)
      if (this@Manager.mapWeakPlayableToTarget[it.playable] == it.target) {
        restorePlaybackInfo(it)
        // this.prepare() ⬇
        if (it.token?.shouldPrepare() == true) it.prepare()
      }
      it.onActive()
      this@Manager.dispatchRefreshAll()
    } ?: throw IllegalStateException("Target is available but Playback is not found: $target")
  }

  // Called when a Playback's target is detached. Eg: PlayerView is detached from window.
  fun onTargetUnAvailable(target: Any) {
    mapTargetToPlayback[target]?.let {
      mapDetachedPlaybackToTime[it] = System.nanoTime()
      val toInActive = mapAttachedPlaybackToTime.remove(it) != null
      it.pause()
      this@Manager.dispatchRefreshAll()
      if (toInActive) it.onInActive()
      if (this@Manager.mapWeakPlayableToTarget[it.playable] == it.target) {
        savePlaybackInfo(it)
        it.release()
      }
    }
  }

  // Called when something has changed about the Playback. Eg: playback's target has layout change.
  fun <T> onPlaybackInternalChanged(playback: Playback<T>) {
    if (!isScrolling()) {
      if (playback.token?.shouldPlay() == true) this.dispatchRefreshAll()
    }
  }

  // Permanently remove the Playback from any cache.
  // Notice: never call this inside an iteration of the maps below:
  // - mapTargetToPlayback
  // - mapAttachedPlaybackToTime
  // - mapDetachedPlaybackToTime
  // - mapPlayableTagToInfo
  fun performDestroyPlayback(playback: Playback<*>) {
    val toInActive = mapAttachedPlaybackToTime.remove(playback) != null
    mapDetachedPlaybackToTime.remove(playback)
    mapTargetToPlayback.remove(playback.target)?.also {
      if (toInActive) it.onInActive()
    } ?: if (toInActive) playback.onInActive()
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
    val cache = this.mapTargetToPlayback[target]
    var shouldAdd = target != null // playback must have a valid target.
    if (shouldAdd) {
      // Not-null target may be already a target of another Playback before.
      // Here we also make sure if we need to torn that old Playback down first or not.
      shouldAdd = cache == null || cache !== playback
    }

    if (shouldAdd) {
      if (cache != null) performDestroyPlayback(cache)
      this.mapTargetToPlayback[target] = playback
      playback.onAdded()
    }

    // In case we are adding existing one (should not happen, but for safety).
    if (mapAttachedPlaybackToTime.containsKey(playback)) {
      // shouldQueue is true when the target is not null and no pre-exist playback.
      if (shouldAdd && playback.token?.shouldPlay() == true) this.dispatchRefreshAll()
    }

    return playback
  }

  // Important. Do the refresh stuff. Change the playback items, etc.
  /**
   * This method is triggered once there is scroll change in the UI Hierarchy, including changing from
   * idle to scroll or vice versa.
   *
   * This method is also be called when there is update in the content, for example Target of a Playback
   * is attached/detached or the Manager itself is attached to the Window.
   *
   * Design target:
   *
   * - [1] Update current attached/detached Playbacks.
   * - [2] Pause the Playbacks those are out of interest (eg: not visible enough).
   * - [3] Allow client to select Playback that is available for a playback.
   */
  fun performRefreshAll() {
    candidates.clear()

    // Confirm if any detached view is re-attached to the Window. Not happen all the time.
    mapDetachedPlaybackToTime.keys.filter { it.token != null }.also { items ->
      dispatcher?.apply {
        // In case the container is non-RecyclerView (so all View are attached to Parent),
        // By refreshing the list, we detect Views those are out of sight, and those are not.
        items.forEach { playback -> this.dispatchTargetAvailable(playback) }
      }
    }

    // List of all attached Playbacks.
    val playbacks = ArrayList(mapAttachedPlaybackToTime.keys)
    // Filter out the bad ones.
    for (playback in playbacks) {
      // Get token here and use later, rather than getting it many times.
      val token = playback.token
      if (token == null) { // Should not null here, but still checking for safety.
        // In case the container is non-RecyclerView (so all View are attached to Parent),
        // By refreshing the list, we detect Views those are out of sight, and those are not.
        dispatcher?.dispatchTargetUnAvailable(playback)
      } else if (token.shouldPlay()) {
        // This will also sort the playback using Token's comparator.
        candidates[token] = playback
      }
    }

    val toPlay = playerSelector.select(ArrayList(candidates.values))
    playbacks.removeAll(toPlay)
    playbacks.addAll(mapDetachedPlaybackToTime.keys)
    // Keep only non-play-candidate ones, and pause them.
    for (playback in playbacks) {
      playback.pause()
    }
    // Now start the play-candidate
    if (!isScrolling()) {
      for (playback in toPlay) {
        playback.play()
      }
    }
    // Clean up cache
    candidates.clear()
  }

}