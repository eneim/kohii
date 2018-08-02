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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import kohii.v1.exo.Config
import kohii.v1.exo.ExoStore
import kohii.v1.exo.MediaSourceFactory
import kohii.v1.exo.PlayerFactory
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
class Kohii(context: Context) {

  internal val app = context.applicationContext as Application
  internal val store = ExoStore[app]

  // Map between Context with the Manager that is hosted on it.
  internal val mapWeakContextToManager = WeakHashMap<Context, Manager>()

  // Which Playable is managed by which Manager
  internal val mapWeakPlayableToManager = WeakHashMap<Playable, Manager?>()

  // Store playables whose tag is available. Non tagged playables are always ignored.
  internal val mapTagToPlayable = HashMap<Any /* ← playable tag */, Playable>()

  init {
    app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity?, state: Bundle?) {
      }

      override fun onActivityStarted(activity: Activity) {
        mapWeakContextToManager[activity]?.onHostStarted()
      }

      override fun onActivityResumed(activity: Activity) {
      }

      override fun onActivityPaused(activity: Activity) {
      }

      override fun onActivityStopped(activity: Activity) {
        mapWeakContextToManager[activity]?.onHostStopped(activity.isChangingConfigurations)
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      }

      // Note: [20180527] This method is called before DecorView is detached.
      override fun onActivityDestroyed(activity: Activity) {
        mapWeakContextToManager.remove(activity)?.onHostDestroyed()
      }
    })
  }

  internal class ManagerAttachStateListener(context: Context, val view: View) :
      View.OnAttachStateChangeListener {

    private val weakContext = WeakReference(context)

    override fun onViewAttachedToWindow(v: View) {
      kohii!!.mapWeakContextToManager[weakContext.get()]?.onAttached()
    }

    // Will get called after Activity's onDestroy().
    override fun onViewDetachedFromWindow(v: View) {
      kohii!!.mapWeakContextToManager.remove(weakContext.get())?.onDetached()
      if (this.view === v) this.view.removeOnAttachStateChangeListener(this)
    }
  }

  internal class GlobalScrollChangeListener(val manager: Manager) : OnScrollChangedListener {

    val scrollConsumed = AtomicBoolean(false)
    val managerRef = WeakReference(manager)

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        val manager = managerRef.get() ?: return
        when (msg.what) {
          EVENT_SCROLL -> {
            manager.setScrolling(true)
            manager.dispatchRefreshAll()
            this.sendEmptyMessageDelayed(EVENT_IDLE, EVENT_DELAY)
          }
          EVENT_IDLE -> {
            manager.setScrolling(false)
            manager.dispatchRefreshAll()
          }
        }
      }
    }

    override fun onScrollChanged() {
      scrollConsumed.set(false)
      handler.removeMessages(EVENT_IDLE)
      handler.removeMessages(EVENT_SCROLL)
      handler.sendEmptyMessageDelayed(EVENT_SCROLL, EVENT_DELAY)
    }

    companion object {
      private const val EVENT_SCROLL = 1
      private const val EVENT_IDLE = 2
      private const val EVENT_DELAY = (3 * 1000 / 60).toLong()  // 50 ms, 3 frames
    }
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var kohii: Kohii? = null

    operator fun get(context: Context) = kohii ?: synchronized(Kohii::class.java) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    operator fun get(fragment: Fragment) = get(fragment.requireActivity())
  }

  //// instance methods

  internal fun getManager(context: Context): Manager {
    if (context !is Activity) {
      throw RuntimeException("Expect Activity, found: " + context.javaClass.simpleName)
    }

    val decorView = context.window.peekDecorView()  //
        ?: throw IllegalStateException("DecorView is null")
    return mapWeakContextToManager[context] ?: Manager.Builder(this, decorView).build()  //
        .also {
          mapWeakContextToManager[context] = it
          if (ViewCompat.isAttachedToWindow(decorView)) it.onAttached()
          decorView.addOnAttachStateChangeListener(ManagerAttachStateListener(context, decorView))
        }
  }

  // Acquire from cache or build new one. The result must not be mapped to any Manager.
  // If the builder has no valid tag (a.k.a tag is null), then return new one.
  internal fun acquirePlayable(uri: Uri, builder: Playable.Builder): Playable {
    val tag = builder.tag
    return (
        if (tag != null) (mapTagToPlayable[tag] ?: PlayableImpl(this, uri,
            builder).also { mapTagToPlayable[tag] = it })  // only save to store for when tag is valid
        else
          PlayableImpl(this, uri, builder)
        ) // ↑ Playable instance obtained. Next: clear its history ↓
        .also { mapWeakPlayableToManager[it] = null }
  }

  // Called when a Playable is no longer be managed by any Manager, its resource should be release.
  internal fun releasePlayable(tag: Any?, playable: Playable) {
    tag?.run {
      if (mapTagToPlayable.remove(tag) != playable) {
        throw IllegalArgumentException("Illegal playable removal: $playable")
      }
    } // TODO release Playable when tag is null.
  }

  internal fun onManagerActiveForPlayback(manager: Manager, playback: Playback<*>) {
    mapWeakPlayableToManager[playback.playable] = manager
    if (manager.mapPlayableToTarget[playback.playable] == playback.getTarget()) {
      manager.restorePlaybackInfo(playback)
      playback.prepare()
    }
    playback.onTargetAvailable()
  }

  internal fun onManagerInActiveForPlayback(manager: Manager, playback: Playback<*>,
      configChange: Boolean) {
    val playable = playback.playable
    // Only pause this playback if the playable is managed by this manager, or by no-one else.
    if (mapWeakPlayableToManager[playable] == manager || mapWeakPlayableToManager[playable] == null) {
      if (!configChange) {
        playback.pause()
        manager.savePlaybackInfo(playback)
      }
    }

    if (!configChange) {
      if (mapWeakPlayableToManager[playable] == manager) mapWeakPlayableToManager[playable] = null
    } else {
      // TODO [20180719] double check this case. We may not need this.
      mapWeakPlayableToManager[playable] = manager
    }
  }

  //// [BEGIN] Public API

  fun setUp(uri: Uri): Playable.Builder {
    return Playable.Builder(this, uri)
  }

  fun setUp(url: String): Playable.Builder {
    return this.setUp(Uri.parse(url))
  }

  // Find a Playable for a tag. Single player may use this for full-screen playback.
  // TODO [20180719] result Playable is still managed by a Manager. Need to double check.
  fun findPlayable(tag: Any?): Playable? {
    return this.mapTagToPlayable[tag].also {
      mapWeakPlayableToManager[it] = null
    }
  }

  @Suppress("unused")
  fun addPlayerFactory(config: Config, playerFactory: PlayerFactory) {
    store.playerFactories[config] = playerFactory
  }

  @Suppress("MemberVisibilityCanBePrivate", "unused")
  fun addMediaSourceFactory(config: Config, mediaSourceFactory: MediaSourceFactory) {
    store.sourceFactories[config] = mediaSourceFactory
  }

  //// [END] Public API
}