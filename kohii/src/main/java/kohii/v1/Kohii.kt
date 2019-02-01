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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.view.Window
import android.view.Window.Callback
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.exoplayer2.ExoPlayerLibraryInfo.VERSION_SLASHY
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.common.WindowCallbackWrapper
import kohii.media.Media
import kohii.media.MediaItem
import kohii.v1.exo.DefaultBandwidthMeterFactory
import kohii.v1.exo.DefaultBridgeProvider
import kohii.v1.dummy.DummyBridgeProvider
import kohii.v1.exo.DefaultDrmSessionManagerProvider
import kohii.v1.exo.DefaultMediaSourceFactoryProvider
import kohii.v1.exo.DefaultPlayerProvider
import kohii.v1.exo.MediaSourceFactoryProvider
import kohii.v1.exo.PlayerProvider
import java.io.File
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author eneim (2018/06/24).
 */
class Kohii(context: Context) : LifecycleObserver {

  internal val app = context.applicationContext as Application

  // Map between Window with the Manager that is hosted on it.
  internal val mapWeakWindowToManager = WeakHashMap<Window, Manager>()

  // Which Playable is managed by which Manager
  internal val mapWeakPlayableToManager = WeakHashMap<Playable, Manager?>()

  // Store playable whose tag is available. Non tagged playable are always ignored.
  internal val mapTagToPlayable = HashMap<Any /* ⬅ playable tag */, Playable>()

  internal val bridgeProvider: BridgeProvider

  private val playerProvider: PlayerProvider
  private val mediaSourceFactoryProvider: MediaSourceFactoryProvider

  init {
    app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityCreated(
        activity: Activity,
        state: Bundle?
      ) {
        // On recreation, we actively create and cache a Manager to prevent bad things to Playables.
        if (state != null) fetchManager(activity.window)
      }

      override fun onActivityStarted(activity: Activity) {
        mapWeakWindowToManager[activity.window]?.onHostStarted()
      }

      override fun onActivityResumed(activity: Activity) = Unit

      override fun onActivityPaused(activity: Activity) = Unit

      // Note [20181021]: (eneim) Considered to free unused resource here, due to the nature of onDestroy.
      // But this method is also called when User click to "Current Apps" thing, and releasing
      // stuff there is not good for UX.
      override fun onActivityStopped(activity: Activity) {
        mapWeakWindowToManager[activity.window]?.onHostStopped(activity.isChangingConfigurations)
      }

      override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
      ) = Unit

      // Note [20180527]: (eneim) This method is called before DecorView is detached.
      // Note [20181021]: (eneim) I also try to clean up any unused Player instances here.
      // I acknowledge that onDestroy() can be ignored by System. But it is the case where the
      // whole process is also destroyed. So nothing on-memory will remain.
      override fun onActivityDestroyed(activity: Activity) {
        mapWeakWindowToManager.remove(activity.window)
            ?.let {
              it.onHostDestroyed()
              it.onDetached() // Manager should not outlive Activity, any second.
            }

        // Activity is not being recreated, and there is no Manager available.
        if (!activity.isChangingConfigurations && mapWeakWindowToManager.isEmpty()) {
          this@Kohii.cleanUp()
        }
      }
    })

    // TODO [20190201] Use this to check screen on/off state.
    // ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    // Shared dependencies.
    val userAgent = getUserAgent(this.app, BuildConfig.LIB_NAME)
    val bandwidthMeter = DefaultBandwidthMeter()
    val httpDataSource = DefaultHttpDataSourceFactory(userAgent, bandwidthMeter.transferListener)

    // ExoPlayerProvider
    val drmSessionManagerProvider = DefaultDrmSessionManagerProvider(this.app, httpDataSource)
    playerProvider = DefaultPlayerProvider(
        this.app,
        DefaultBandwidthMeterFactory(),
        drmSessionManagerProvider
    )

    // MediaSourceFactoryProvider
    var tempDir = this.app.getExternalFilesDir(null)
    if (tempDir == null) tempDir = this.app.filesDir
    val fileDir = tempDir
    val contentDir = File(fileDir, CACHE_CONTENT_DIRECTORY)
    val mediaCache = SimpleCache(contentDir, LeastRecentlyUsedCacheEvictor(CACHE_SIZE))
    val upstreamFactory = DefaultDataSourceFactory(this.app, httpDataSource)
    mediaSourceFactoryProvider = DefaultMediaSourceFactoryProvider(upstreamFactory, mediaCache)

    bridgeProvider = DefaultBridgeProvider(playerProvider, mediaSourceFactoryProvider)
    // bridgeProvider = DummyBridgeProvider()
  }

  internal class ManagerAttachStateListener(
    window: Window,
    val view: View
  ) : View.OnAttachStateChangeListener {

    private val weakWindow = WeakReference(window)

    override fun onViewAttachedToWindow(v: View) {
      kohii!!.mapWeakWindowToManager[weakWindow.get()]?.onAttached()
    }

    // Will get called after Activity's onDestroy().
    override fun onViewDetachedFromWindow(v: View) {
      // The next line may not be called, we may already remove this in onActivityDestroyed.
      kohii!!.mapWeakWindowToManager.remove(weakWindow.get())
          ?.onDetached()
      if (this.view === v) this.view.removeOnAttachStateChangeListener(this)
    }
  }

  internal class GlobalScrollChangeListener(val manager: Manager) : OnScrollChangedListener {

    private val scrollConsumed = AtomicBoolean(false)
    val weakManager = WeakReference(manager)

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        val manager = weakManager.get() ?: return
        when (msg.what) {
          EVENT_SCROLL -> {
            manager.setScrolling(true)
            // manager.dispatchRefreshAll()
            this.sendEmptyMessageDelayed(EVENT_IDLE, EVENT_DELAY)
          }
          EVENT_IDLE -> {
            manager.setScrolling(false)
            // manager.dispatchRefreshAll()
          }
        }

        // TODO double check this behavior.
        if (!scrollConsumed.getAndSet(true)) {
          manager.dispatchRefreshAll()
        }
      }
    }

    override fun onScrollChanged() {
      scrollConsumed.set(false)
      handler.removeCallbacksAndMessages(null)
      handler.sendEmptyMessageDelayed(EVENT_SCROLL, EVENT_DELAY)
    }
  }

  class ScreenOnOffReceiver : BroadcastReceiver() {
    override fun onReceive(
      context: Context?,
      intent: Intent?
    ) {
      if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {
        // TODO pause all
      } else if (intent?.action.equals(Intent.ACTION_SCREEN_ON)) {
        // TODO restart all.
      }
    }
  }

  // Prepare.
  @Suppress("unused")
  internal class WindowCallback(origin: Callback) : WindowCallbackWrapper(origin)

  @Suppress("unused")
  companion object {
    private const val TAG = "Kohii:X"
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 32 * 1024 * 1024L // 32 Megabytes

    private const val EVENT_SCROLL = 1
    private const val EVENT_IDLE = 2
    private const val EVENT_DELAY = (1 * 1000 / 60).toLong()  // 1 frames

    @Volatile
    private var kohii: Kohii? = null

    // So we can call Kohii[context] instead of Kohii.get(context).
    operator fun get(context: Context) = kohii ?: synchronized(Kohii::class.java) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    operator fun get(fragment: Fragment) = get(fragment.requireActivity().window)

    // Not many place we can have a Window, other than Activity and Dialog.
    // In practice, client must call this from Dialog or Activity, so it will create a Manager in advance.
    operator fun get(window: Window) = get(window.context).also { it.requireManager(window) }

    // ExoPlayer's doesn't catch a RuntimeException and crash if Device has too many App installed.
    internal fun getUserAgent(
      context: Context,
      appName: String
    ): String {
      val versionName = try {
        val packageName = context.packageName
        val info = context.packageManager.getPackageInfo(packageName, 0)
        info.versionName
      } catch (e: Exception) {
        "?"
      }

      return "$appName/$versionName (Linux;Android ${Build.VERSION.RELEASE}) $VERSION_SLASHY"
    }
  }

  //// instance methods

  internal fun fetchManager(window: Window): Manager? {
    return window.peekDecorView()
        ?.let {
          requireManager(window)
        }
  }

  internal fun requireManager(window: Window): Manager {
    // Peek to read, not to create new.
    val decorView = requireNotNull(window.peekDecorView()) { "DecorView is null for $window" }
    return mapWeakWindowToManager[window] ?: //
    Manager.Builder(this, decorView).build().also {
      mapWeakWindowToManager[window] = it
      if (ViewCompat.isAttachedToWindow(decorView)) it.onAttached()
      decorView.addOnAttachStateChangeListener(ManagerAttachStateListener(window, decorView))
    }
  }

  // If called by Context, it must be Activity.
  internal fun requireManager(context: Context): Manager {
    return mapWeakWindowToManager.entries.firstOrNull { it.key.context === context }?.value // cache
        ?: (requireManager( // no cache, now create new using context. will fail for non-Activity.
            (context as? Activity)?.window ?: throw RuntimeException(
                "Expect Activity, got: " + context.javaClass.simpleName
            )
        ))
  }

  // Called when a Playable is no longer be managed by any Manager, its resource should be release.
  // Always get called after playback.release()
  internal fun releasePlayable(
    tag: Any?,
    playable: Playable
  ) {
    tag?.run {
      if (mapTagToPlayable.remove(tag) !== playable) {
        throw IllegalArgumentException("Illegal playable removal: $playable")
      }
    } ?: playable.release()
  }

  // Gat called when Kohii should free all resources.
  // FIXME [20180805] Now, this is called when activity destroy is detected, and there is no
  // Manager be alive. In the future, we may consider to have non-UI Manager.
  internal fun cleanUp() {
    playerProvider.cleanUp()
  }

  //// [BEGIN] Public API

  fun setUp(uri: Uri): Playable.Builder {
    return this.setUp(MediaItem(uri))
  }

  fun setUp(url: String): Playable.Builder {
    return this.setUp(MediaItem(Uri.parse(url)))
  }

  fun setUp(media: Media): Playable.Builder {
    return Playable.Builder(this, media = media)
  }

  // Find a Playable for a tag. Single player may use this for full-screen playback.
  // TODO [20180719] returned Playable may still be managed by a Manager. Need to know why.
  fun findPlayable(tag: Any?): Playable? {
    return this.mapTagToPlayable[tag].also {
      mapWeakPlayableToManager[it] = null // once found, it will be detached from the last Manager.
    }
  }

  //// [END] Public API
}