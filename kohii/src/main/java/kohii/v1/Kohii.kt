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
import android.support.v4.view.ViewCompat
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import kohii.v1.exo.ExoStore
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

  internal val store = ExoStore.get(app)
  internal val managers = WeakHashMap<Context, Manager>()
  internal val states = WeakHashMap<Context, Bundle>()  // TODO: rename to 'playableStates'
  internal val playableStore = HashMap<Playable.Bundle, Playable>()
  internal val playablePacks = HashMap<Any, Playable>()
  internal val referenceQueue = ReferenceQueue<Any>()

  init {
    app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity?, state: Bundle?) {
        val cache = state?.getBundle(KEY_ACTIVITY_STATES)
        states[activity] = cache  // accept null, just won't use it.
      }

      override fun onActivityStarted(activity: Activity) {
        managers[activity]?.onStart()
      }

      override fun onActivityResumed(activity: Activity) {
      }

      override fun onActivityPaused(activity: Activity) {
      }

      override fun onActivityStopped(activity: Activity) {
        managers[activity]?.onStop(activity.isChangingConfigurations)
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        managers[activity]?.onSavePlaybackInfo()?.apply {
          outState.putBundle(KEY_ACTIVITY_STATES, this)
        }
      }

      // [20180527] This method is called before DecorView is detached.
      override fun onActivityDestroyed(activity: Activity) {
        managers.remove(activity)?.onDestroy(activity.isChangingConfigurations)
      }
    })
  }

  internal class ManagerAttachStateListener(context: Context, val view: View,
      private val attachFlag: AtomicBoolean) : View.OnAttachStateChangeListener {

    private val context = WeakReference(context)

    override fun onViewAttachedToWindow(v: View) {
      if (!attachFlag.get()) {
        val toAttach = kohii!!.managers[context.get()]
        if (toAttach != null) {
          toAttach.onAttached()
          attachFlag.set(true)
        }
      }
    }

    override fun onViewDetachedFromWindow(v: View) {
      if (attachFlag.get()) {
        val toDetach = kohii!!.managers.remove(context.get())
        if (toDetach != null) {
          toDetach.onDetached()
          attachFlag.set(false)
        }
      }

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

    fun with(context: Context) = kohii ?: synchronized(this) {
      kohii ?: Kohii(context).also { kohii = it }
    }
  }

  //// instance methods

  internal fun getManager(context: Context): Manager {
    if (context !is Activity) {
      throw RuntimeException("Expect Activity, found: " + context.javaClass.simpleName)
    }

    val decorView = context.window.peekDecorView() ?: throw IllegalStateException(
        "DecorView is null")
    val manager = managers[context] ?: Manager(this, decorView)
        .also {
          managers[context] = it
          if (ViewCompat.isAttachedToWindow(decorView)) it.onAttached()
          decorView.addOnAttachStateChangeListener(
              ManagerAttachStateListener(context, decorView, it.attachFlag))
        }

    manager.onInitialized(states[context])
    return manager
  }

  internal fun getPlayable(bundle: Playable.Bundle): Playable {
    return playableStore[bundle] ?: Playee(this, store, bundle).also {
      playableStore[bundle] = it
    }
  }

  //// [BEGIN] Public API

  fun setUp(uri: Uri): Playable.Options {
    return Playable.Options(this, uri)
  }

  fun requirePlayable(key: Any): Playable? {
    return this.playablePacks[checkNotNull(key)]
  }

  //// [END] Public API
}