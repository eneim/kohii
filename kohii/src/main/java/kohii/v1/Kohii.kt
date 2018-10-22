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
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import kohii.v1.exo.ExoPlayable
import kohii.v1.exo.ExoStore
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
class Kohii(context: Context) {

  internal val app = context.applicationContext as Application
  internal val exoStore = ExoStore[app]

  // Map between Context with the Manager that is hosted on it.
  internal val mapWeakContextToManager = WeakHashMap<Context, Manager>()

  // Which Playable is managed by which Manager
  internal val mapWeakPlayableToManager = WeakHashMap<Playable, Manager?>()

  // Store playables whose tag is available. Non tagged playables are always ignored.
  private val mapTagToPlayable = HashMap<Any /* ⬅ playable tag */, Playable>()

  init {
    app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, state: Bundle?) {
        Log.i(TAG, "onActivityCreated() called: ${activity.javaClass.simpleName}, $state")
      }

      override fun onActivityStarted(activity: Activity) {
        mapWeakContextToManager[activity]?.onHostStarted()
        Log.i(TAG, "onActivityStarted() called: ${activity.javaClass.simpleName}")
      }

      override fun onActivityResumed(activity: Activity) {
        // Ignored
      }

      override fun onActivityPaused(activity: Activity) {
        // Ignored
      }

      // Note [20181021]: (eneim) Considered to free unused resource here, due to the nature of onDestroy. But this method is also called when User click to "Current Apps" thing, and releasing stuff there is not good for UX.
      override fun onActivityStopped(activity: Activity) {
        mapWeakContextToManager[activity]?.onHostStopped(activity.isChangingConfigurations)
        Log.i(TAG, "onActivityStopped() called: ${activity.javaClass.simpleName}")
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      }

      // Note [20180527]: (eneim) This method is called before DecorView is detached.
      // Note [20181021]: (eneim) I also try to clean up any unused Player instances here. I acknowledge that onDestroy() can be ignored by System. But it is the case where the whole process is also destroyed. So nothing on-memory will remain.
      override fun onActivityDestroyed(activity: Activity) {
        Log.i(TAG, "onActivityDestroyed() called: ${activity.javaClass.simpleName}, "
            + "${activity.isChangingConfigurations}")

        mapWeakContextToManager.remove(activity)?.let {
          it.onHostDestroyed()
          it.onDetached() // Manager should not outlive Activity, any second.
        }

        // Debug only.
        if (BuildConfig.DEBUG) {
          mapWeakPlayableToManager.forEach {
            if (it.value == null) {
              Log.w(TAG, "onActivityDestroyed(): ${it.key} -- ${it.value}")
            } else {
              Log.d(TAG, "onActivityDestroyed(): ${it.key} -- ${it.value}")
            }
          }
        }

        // Activity is not being recreated, and there is no Manager available.
        if (!activity.isChangingConfigurations && mapWeakContextToManager.isEmpty()) {
          this@Kohii.cleanUp()
        }
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
      // The next line may not be called, we may already remove this in onActivityDestroyed.
      kohii!!.mapWeakContextToManager.remove(weakContext.get())?.onDetached()
      if (this.view === v) this.view.removeOnAttachStateChangeListener(this)
    }
  }

  // TODO [20181022] Is it safe to use this class for Activity's lifetime?
  internal class GlobalScrollChangeListener(val manager: Manager) : OnScrollChangedListener {

    private val scrollConsumed = AtomicBoolean(false)
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
    private const val TAG = "Kohii:X"

    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var kohii: Kohii? = null

    // So we can call Kohii[context] instead of Kohii.get(context).
    operator fun get(context: Context) = kohii ?: synchronized(Kohii::class.java) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    operator fun get(fragment: Fragment) = get(fragment.requireActivity())
  }

  //// instance methods

  internal fun getManager(context: Context): Manager {
    if (context !is Activity) {
      throw RuntimeException("Expect Activity, got: " + context.javaClass.simpleName)
    }

    val decorView = context.window.peekDecorView()  // peek to read, not create
        ?: throw IllegalStateException("DecorView is null for $context")
    return mapWeakContextToManager[context] ?: //
    Manager.Builder(this, decorView).build().also {
      mapWeakContextToManager[context] = it
      if (ViewCompat.isAttachedToWindow(decorView)) it.onAttached()
      decorView.addOnAttachStateChangeListener(ManagerAttachStateListener(context, decorView))
    }
  }

  // Acquire Playable from cache or build new one. The result must not be mapped to any Manager.
  // If the builder has no valid tag (a.k.a tag is null), then always return new one.
  // TODO [20181021] Consider to make this to use the Factory mechanism?
  internal fun acquirePlayable(uri: Uri, builder: Playable.Builder): Playable {
    val tag = builder.tag
    return (
        if (tag != null) (mapTagToPlayable[tag] ?: ExoPlayable(this, uri, builder)
            .also { mapTagToPlayable[tag] = it })  // only save to store for when tag is valid
        else
          ExoPlayable(this, uri, builder)
        ) // ↑ Playable instance obtained. Next: clear its history ↓
        .also { mapWeakPlayableToManager[it] = null }
  }

  // Called when a Playable is no longer be managed by any Manager, its resource should be release.
  // Always get called after playable.release()
  internal fun releasePlayable(tag: Any?, playable: Playable) {
    tag?.run {
      if (mapTagToPlayable.remove(tag) != playable) {
        throw IllegalArgumentException("Illegal playable removal: $playable")
      }
    } // TODO release Playable when tag is null.
  }

  /** Called when a Manager becomes active to a Playback that it already manages. */
  internal fun onManagerActiveForPlayback(manager: Manager, playback: Playback<*>) {
    mapWeakPlayableToManager[playback.playable] = manager
    // Check if the Playable is already bound to the Playback's target.
    if (manager.mapWeakPlayableToTarget[playback.playable] == playback.target) {
      manager.restorePlaybackInfo(playback)
      // TODO [20180905] double check the usage of 'shouldPrepare()' here.
      if (playback.token?.shouldPrepare() == true) playback.prepare()
    }
    playback.onTargetAvailable()
  }

  /**
   * Called when the Activity mapped to a Manager is stopped. When called, a [Playback]'s target is
   * considered 'unavailable' even if it is not detached yet (in case the target is a [View])
   *
   * @param manager The [Manager] whose Activity is stopped.
   * @param playback The [Playback] that is managed by the manager.
   * @param configChange If true, the Activity is being recreated by a config change, false otherwise.
   */
  internal fun onManagerInActiveForPlayback(
      manager: Manager,
      playback: Playback<*>,
      configChange: Boolean
  ) {
    val playable = playback.playable
    // Only pause this playback if [1] config change is happening and [2] the playable is managed by this manager, or by no-one else.
    // FYI: The Playable instances holds the actual playback resource. It is not managed by anything else when the Activity is destroyed and to be recreated (config change).
    if (mapWeakPlayableToManager[playable] == manager || mapWeakPlayableToManager[playable] == null) {
      if (!configChange) {
        playback.pause()
        manager.savePlaybackInfo(playback)
      }
    }
    playback.onTargetUnAvailable()
    // There is no recreation. If the manager is managing the playable, unload the Playable.
    if (!configChange) {
      if (mapWeakPlayableToManager[playable] == manager) mapWeakPlayableToManager[playable] = null
    }
  }

  // Gat called when Kohii should free all resources.
  // FIXME [20180805] Now, this is called when activity destroy is detected, and there is no further
  // Manager to be alive. In the future, we may consider to have non-Activity Manager.
  internal fun cleanUp() {
    exoStore.cleanUp()
  }

  //// [BEGIN] Public API

  fun setUp(uri: Uri): Playable.Builder {
    return Playable.Builder(this, uri)
  }

  fun setUp(url: String): Playable.Builder {
    return this.setUp(Uri.parse(url))
  }

  // Find a Playable for a tag. Single player may use this for full-screen playback.
  // TODO [20180719] returned Playable may still be managed by a Manager. Need to double check.
  fun findPlayable(tag: Any?): Playable? {
    return this.mapTagToPlayable[tag].also {
      mapWeakPlayableToManager[it] = null // once found, it will be detached from the last Manager.
    }
  }

  //// [END] Public API
}