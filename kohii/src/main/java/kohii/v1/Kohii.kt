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

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.google.android.exoplayer2.ExoPlayerLibraryInfo.VERSION_SLASHY
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.internal.ViewPlaybackManager
import kohii.media.Media
import kohii.media.MediaItem
import kohii.takeFirstOrNull
import kohii.v1.exo.DefaultBandwidthMeterFactory
import kohii.v1.exo.DefaultBridgeProvider
import kohii.v1.exo.DefaultDrmSessionManagerProvider
import kohii.v1.exo.DefaultMediaSourceFactoryProvider
import kohii.v1.exo.DefaultPlayerProvider
import kohii.v1.exo.MediaSourceFactoryProvider
import kohii.v1.exo.PlayerProvider
import java.io.File
import java.util.WeakHashMap

/**
 * @author eneim (2018/06/24).
 */
@Suppress("MemberVisibilityCanBePrivate")
class Kohii(context: Context) {

  internal val app = context.applicationContext as Application

  internal val owners = HashMap<LifecycleOwner, ActivityContainer>(2)
  internal val managers = HashMap<LifecycleOwner, PlaybackManager>()

  // Which Playable is managed by which Manager
  internal val mapPlayableToManager = WeakHashMap<Playable<*>, PlaybackManager?>()

  // Store playable whose tag is available. Non tagged playable are always ignored.
  internal val mapTagToPlayable = HashMap<Any /* ⬅ playable tag */, Playable<*>>()

  // For ExoPlayer resource management.
  internal val bridgeProvider: BridgeProvider
  internal val playerProvider: PlayerProvider
  internal val mediaSourceFactoryProvider: MediaSourceFactoryProvider

  internal val screenStateReceiver by lazy {
    ScreenStateReceiver()
  }

  companion object {
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 32 * 1024 * 1024L // 32 Megabytes

    @Volatile private var kohii: Kohii? = null

    operator fun get(context: Context) = kohii ?: synchronized(Kohii::class.java) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    operator fun get(fragment: Fragment) = get(fragment.requireContext())

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

  init {
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

  //// instance methods

  internal fun findSuitableManager(target: Any): PlaybackManager? {
    return owners.values.takeFirstOrNull({ it.findSuitableManger(target) }, { it != null })
  }

  // Called when a Playable is no longer be managed by any Manager, its resource should be release.
  // Always get called after playback.release()
  internal fun releasePlayable(
    tag: Any?,
    playable: Playable<*>
  ) {
    tag?.run {
      val cache = mapTagToPlayable.remove(tag)
      if (cache !== playable) {
        throw IllegalArgumentException(
            "Illegal playable removal: cached playable of tag [$tag] is [$cache] but not [$playable]"
        )
      }
    } ?: playable.release()
  }

  internal fun onFirstManagerOnline() {
    val intentFilter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_SCREEN_OFF)
    }
    app.registerReceiver(screenStateReceiver, intentFilter)
  }

  internal fun onLastManagerOffline() {
    app.unregisterReceiver(screenStateReceiver)
  }

  // Gat called when Kohii should free all resources.
  internal fun cleanUp() {
    playerProvider.cleanUp()
  }

  //// [BEGIN] Public API

  // Expose to client.
  fun register(provider: LifecycleOwnerProvider) {
    return this.register(provider, null)
  }

  fun register(
    provider: LifecycleOwnerProvider,
    containers: Array<Any>?
  ) {
    val activity =
      when (provider) {
        is Fragment -> provider.requireActivity()
        is FragmentActivity -> provider
        else -> throw IllegalArgumentException(
            "Unsupported provider: $provider. Kohii only supports Fragment, FragmentActivity."
        )
      }
    val parent = owners.getOrPut(activity) {
      val result = ActivityContainer(this, activity)
      activity.lifecycle.addObserver(result)
      return@getOrPut result
    }
    val lifecycleOwner = provider.provideLifecycleOwner()
    val manager = managers.getOrPut(lifecycleOwner) {
      // Create new in case of no cache found.
      if (lifecycleOwner is LifecycleService) {
        // ServicePlaybackManager(this, provider)
        throw IllegalArgumentException(
            "Service is not supported yet."
        )
      } else {
        val result = ViewPlaybackManager(this, parent, provider)
        containers?.mapNotNull { Container.createContainer(it, result) }
            ?.forEach { result.registerContainer(it, false) }
        return@getOrPut result
      }
    }

    lifecycleOwner.lifecycle.addObserver(manager)
    parent.attachPlaybackManager(manager)
  }

  fun setUp(uri: Uri) = this.setUp(MediaItem(uri))

  fun setUp(url: String) = this.setUp(MediaItem(Uri.parse(url)))

  fun setUp(media: Media) = Playable.Builder(this, media = media)

  // Find a Playable for a tag. Single player may use this for full-screen playback.
  fun findPlayable(tag: Any?): Playable<*>? {
    return this.mapTagToPlayable[tag].also {
      mapPlayableToManager[it] = null // once found, it will be detached from the last Manager.
    }
  }

  //// [END] Public API

  //// Interface definitions
}