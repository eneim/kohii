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
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import kohii.v1.Manager.Builder
import kohii.v1.exo.Config
import kohii.v1.exo.ExoStore
import kohii.v1.exo.MediaSourceFactory
import kohii.v1.exo.PlayerFactory
import java.lang.ref.ReferenceQueue
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
  internal val managers = WeakHashMap<Context, Manager>()
  internal val playableStates = WeakHashMap<Context, Bundle>()
  internal val playableStore = HashMap<Playable.Bundle, Playable>()
  internal val referenceQueue = ReferenceQueue<Any>()

  init {
    app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity?, state: Bundle?) {
        val cache = state?.getBundle(KEY_ACTIVITY_STATES)
        playableStates[activity] = cache  // accept null, just won't use it.
      }

      override fun onActivityStarted(activity: Activity) {
        managers[activity]?.onHostStarted()
      }

      override fun onActivityResumed(activity: Activity) {
      }

      override fun onActivityPaused(activity: Activity) {
      }

      override fun onActivityStopped(activity: Activity) {
        managers[activity]?.onHostStopped(activity.isChangingConfigurations)
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        managers[activity]?.onSavePlaybackInfo()?.apply {
          outState.putBundle(KEY_ACTIVITY_STATES, this)
        }
      }

      // [20180527] This method is called before DecorView is detached.
      override fun onActivityDestroyed(activity: Activity) {
        managers.remove(activity)?.onHostDestroyed(activity.isChangingConfigurations)
      }
    })
  }

  internal class ManagerAttachStateListener(context: Context,
      val view: View) : View.OnAttachStateChangeListener {

    private val context = WeakReference(context)

    override fun onViewAttachedToWindow(v: View) {
      kohii!!.managers[context.get()]?.onAttached()
    }

    // May be called after Activity's onDestroy().
    override fun onViewDetachedFromWindow(v: View) {
      kohii!!.managers.remove(context.get())?.onDetached()
      if (this.view === v) this.view.removeOnAttachStateChangeListener(this)
    }
  }

  internal class GlobalScrollChangeListener(manager: Manager) : OnScrollChangedListener {

    val scrollConsumed = AtomicBoolean(false)
    val managerRef = WeakReference(manager)

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        val container = managerRef.get() ?: return
        when (msg.what) {
          EVENT_SCROLL -> {
            container.setScrolling(true)
            this.removeMessages(EVENT_IDLE)
            this.sendEmptyMessageDelayed(EVENT_IDLE, EVENT_DELAY.toLong())
          }
          EVENT_IDLE -> {
            container.setScrolling(false)
            if (scrollConsumed.compareAndSet(false, true)) container.dispatchRefreshAll()
          }
        }
      }
    }

    override fun onScrollChanged() {
      scrollConsumed.set(false)
      handler.removeMessages(EVENT_SCROLL)
      handler.sendEmptyMessageDelayed(EVENT_SCROLL, EVENT_DELAY.toLong())
    }

    companion object {
      private const val EVENT_SCROLL = 1
      private const val EVENT_IDLE = 2
      private const val EVENT_DELAY = 3 * 1000 / 60  // 50 ms, 3 frames
    }
  }

  companion object {
    internal const val KEY_ACTIVITY_STATES = "kohii:activity:states"
    internal const val KEY_MANAGER_STATES = "kohii:manager:states"

    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var kohii: Kohii? = null

    operator fun get(context: Context) = kohii ?: synchronized(this) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    operator fun get(fragment: Fragment) = get(fragment.requireActivity())
  }

  //// instance methods

  internal fun getManager(context: Context): Manager {
    if (context !is Activity) {
      throw RuntimeException("Expect Activity, found: " + context.javaClass.simpleName)
    }

    val decorView = context.window.peekDecorView() ?: throw IllegalStateException(
        "DecorView is null")
    return managers[context] ?: Builder(
        this, decorView
    ).build().also {
      managers[context] = it
      if (ViewCompat.isAttachedToWindow(decorView)) it.onAttached()
      decorView.addOnAttachStateChangeListener(
          ManagerAttachStateListener(context, decorView))
      it.onInitialized(playableStates[context])
    }
  }

  internal fun onManagerStateMayChange(manager: Manager, playable: Playable, active: Boolean) {
    if (active) {
      managers.forEach { it.value.playablesThisActiveTo.remove(playable) }
      manager.playablesThisActiveTo.add(playable)
    } else {
      manager.playablesThisActiveTo.remove(playable)
    }
  }

  // Acquire from cache or build new one.
  internal fun acquirePlayable(bundle: Playable.Bundle): Playable {
    return playableStore[bundle] ?: Playee(this, bundle).also {
      playableStore[bundle] = it
    }
  }

  internal fun releasePlayable(bundle: Playable.Bundle, playable: Playable) {
    if (playableStore.remove(bundle) != playable) {
      throw IllegalArgumentException("Illegal playable removal: $playable")
    }
  }

  //// [BEGIN] Public API

  fun setUp(uri: Uri): Playable.Builder {
    return Playable.Builder(this, uri)
  }

  fun setUp(url: String): Playable.Builder {
    return this.setUp(Uri.parse(url))
  }

  fun findPlayable(tag: Any): Playable? {
    return this.playableStore.find { it.builder.tag == tag }
  }

  @Suppress("unused")
  fun addPlayerFactory(config: Config, playerFactory: PlayerFactory) {
    store.playerFactories[config] = playerFactory
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun addMediaSourceFactory(config: Config, mediaSourceFactory: MediaSourceFactory) {
    store.sourceFactories[config] = mediaSourceFactory
  }

  //// [END] Public API
}

/// Some extension functions.

fun <K, V> HashMap<K, V>.find(predicate: (key: K) -> Boolean): V? {
  return this[this.keys.firstOrNull(predicate)]
}