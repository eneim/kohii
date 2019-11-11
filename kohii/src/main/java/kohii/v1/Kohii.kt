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
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayerLibraryInfo.VERSION_SLASHY
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kohii.Beta
import kohii.ExoPlayer
import kohii.internal.HeadlessPlaybackService
import kohii.internal.ViewPlaybackManager
import kohii.media.Media
import kohii.media.MediaItem
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.exo.DefaultBandwidthMeterFactory
import kohii.v1.exo.DefaultDrmSessionManagerProvider
import kohii.v1.exo.DefaultExoPlayerProvider
import kohii.v1.exo.DefaultMediaSourceFactoryProvider
import kohii.v1.exo.PlayerViewBridgeProvider
import kohii.v1.exo.PlayerViewPlayableCreator
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.Delegates

/**
 * @author eneim (2018/06/24).
 */
inline class PendingState(val value: Boolean)

class Kohii(context: Context) : PlayableManager {

  val app = context.applicationContext as Application

  // Don't be scared. I clean this thing properly.
  internal val groups = ArrayMap<Activity, PlaybackManagerGroup>(2)
  internal val managers = ArrayMap<LifecycleOwner, PlaybackManager>()

  internal val playables = mutableSetOf<Playable<*>>()

  // Map the playback state of Playable made by client (manually)
  // true = the Playable is pending to be started.
  // false = the Playable is pending to be paused.
  internal val manualPlayableRecord by lazy(NONE) { HashMap<Playable<*>, PendingState>() }
  // To mark a Playable as high priority once it is started manually.
  internal val activeManualPlayable by lazy(NONE) { ArraySet<Playable<*>>() }

  // Store playable whose tag is available. Non tagged playable are always ignored.
  internal val mapTagToPlayable = HashMap<Any /* ⬅ playable tag */, Pair<Playable<*>, Class<*>>>()
  // In critical situation, this map may hold a lot of entries, so use HashMap.
  // REQUIRED: Playable Tag is globally unique.
  // TODO consider a 'Paging' approach which may save old records to DB/Files to save RAM.
  internal val mapPlayableTagToInfo = HashMap<Any /* Playable tag */, PlaybackInfo>()

  private var headlessPlayback by Delegates.observable<HeadlessPlayback?>(
      initialValue = null,
      onChange = { _, oldVal, newVal ->
        if (newVal !== oldVal) oldVal?.dismiss()
      })

  @ExoPlayer
  internal val defaultBridgeProvider by lazy(NONE) {
    val userAgent = getUserAgent(this.app, BuildConfig.LIB_NAME)
    val httpDataSource = DefaultHttpDataSourceFactory(userAgent)

    // ExoPlayerProvider
    val drmSessionManagerProvider = DefaultDrmSessionManagerProvider(this.app, httpDataSource)
    val playerProvider = DefaultExoPlayerProvider(
        this.app,
        DefaultBandwidthMeterFactory(),
        drmSessionManagerProvider
    )

    // MediaSourceFactoryProvider
    val fileDir = this.app.getExternalFilesDir(null) ?: this.app.filesDir
    val contentDir = File(fileDir, CACHE_CONTENT_DIRECTORY)
    val mediaCache: Cache = SimpleCache(
        contentDir, LeastRecentlyUsedCacheEvictor(CACHE_SIZE), ExoDatabaseProvider(this.app)
    )
    val upstreamFactory = DefaultDataSourceFactory(this.app, httpDataSource)
    val mediaSourceFactoryProvider = DefaultMediaSourceFactoryProvider(upstreamFactory, mediaCache)
    PlayerViewBridgeProvider(playerProvider, mediaSourceFactoryProvider).also {
      cleanables.add(it)
    }
  }

  @ExoPlayer
  private val defaultPlayableCreator: PlayableCreator<PlayerView> by lazy(NONE) {
    PlayerViewPlayableCreator(this, bridgeProvider = this.defaultBridgeProvider)
  }

  @Suppress("SpellCheckingInspection")
  internal val cleanables = HashSet<Cleanable>()

  private val screenStateReceiver by lazy(NONE) {
    ScreenStateReceiver()
  }

  companion object {
    private const val CACHE_CONTENT_DIRECTORY = "kohii_content"
    private const val CACHE_SIZE = 24 * 1024 * 1024L // 24 Megabytes
    internal val PENDING_PLAY = PendingState(true)
    internal val PENDING_PAUSE = PendingState(false)

    @Volatile private var kohii: Kohii? = null

    @JvmStatic
    operator fun get(context: Context) = kohii ?: synchronized(Kohii::class.java) {
      kohii ?: Kohii(context).also { kohii = it }
    }

    @JvmStatic
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

  // instance methods

  internal fun setHeadlessPlayback(headlessPlayback: HeadlessPlayback?) {
    this.headlessPlayback = headlessPlayback
  }

  internal fun getHeadlessPlayback(): HeadlessPlayback? = this.headlessPlayback

  // TODO revise headless playback. !Important.
  internal fun enterHeadlessPlayback(
    playback: Playback<*>,
    params: HeadlessPlaybackParams
  ) {
    // A HeadlessPlayback will be managed by Kohii.
    playback.playable.manager = this
    val intent = Intent(app, HeadlessPlaybackService::class.java)
    val extras = Bundle().apply {
      putString(HeadlessPlaybackService.KEY_PLAYABLE, playback.tag.toString())
      putParcelable(HeadlessPlaybackService.KEY_PARAMS, params)
    }
    intent.putExtras(extras)
    app.startService(intent)
  }

  // Called when a Playable is no longer be managed by any Manager, its resource should be release.
  // If called, it must be after playback.release()
  internal fun releasePlayable(
    tag: Any?,
    playable: Playable<*>
  ) {
    playable.release()
    tag?.let {
      val cache = mapTagToPlayable.remove(it)
      require(cache?.first === playable) {
        "Illegal cache of tag [$it]: expect [$playable], found [${cache?.first}]."
      }
    }
    manualPlayableRecord.remove(playable)
    activeManualPlayable.remove(playable)
    mapPlayableTagToInfo.remove(playable.tag)
  }

  internal fun onFirstManagerOnline() {
    val intentFilter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_USER_PRESENT)
    }
    app.registerReceiver(screenStateReceiver, intentFilter)
  }

  internal fun onLastManagerOffline() {
    app.unregisterReceiver(screenStateReceiver)
  }

  internal fun play(playback: Playback<*>) {
    val controller = playback.controller
    if (controller != null) {
      playback.doubleCheckHost()
      if (playback.token.shouldPrepare()) playback.playable.prepare()
      manualPlayableRecord[playback.playable] = PENDING_PLAY
      if (!controller.kohiiCanPause()) {
        activeManualPlayable.add(playback.playable)
      }
    }
    playback.manager.dispatchRefreshAll()
  }

  internal fun pause(playback: Playback<*>) {
    val controller = playback.controller
    if (controller != null) {
      manualPlayableRecord[playback.playable] = PENDING_PAUSE
      activeManualPlayable.remove(playback.playable)
    }
    playback.manager.dispatchRefreshAll()
  }

  internal fun shouldCleanUp(): Boolean {
    return this.managers.isEmpty && this.playables.isEmpty()
  }

  // Gat called when Kohii should free all resources.
  internal fun cleanUp() {
    cleanables.onEach { it.cleanUp() }
        .clear()
  }

  internal fun findManagerForContainer(container: Any): PlaybackManager? {
    return managers.values.filter { it.findHostForContainer(container) != null }
        .max() // Manager with highest priority.
  }

  internal fun <RENDERER : Any> cleanUpPool(
    owner: LifecycleOwner,
    rendererPool: RendererPool<RENDERER>
  ) {
    this.managers[owner]?.apply {
      parent.rendererPools.filter { it.value === rendererPool }
          .forEach {
            it.value.cleanUp()
            parent.rendererPools.remove(it.key)
          }
    }
  }

  // TODO check when we should call this method.
  internal fun trySavePlaybackInfo(playable: Playable<*>) {
    if (playable.tag != Playable.NO_TAG) {
      if (!mapPlayableTagToInfo.containsKey(playable.tag)) {
        mapPlayableTagToInfo[playable.tag] = playable.playbackInfo
      }
    }
  }

  // TODO check when we should call this method.
  internal fun tryRestorePlaybackInfo(playable: Playable<*>) {
    if (playable.tag != Playable.NO_TAG) {
      val info = mapPlayableTagToInfo.remove(playable.tag)
      if (info != null) playable.playbackInfo = info
    }
  }

  // [BEGIN] Public API

  // Expose to client.
  fun register(
    container: Any,
    vararg hosts: View
  ): PlaybackManager {
    val (activity, managerLifecycleOwner) =
      when (container) {
        is Fragment -> Pair(container.requireActivity(), container.viewLifecycleOwner)
        is FragmentActivity -> Pair(container, container)
        else -> throw IllegalArgumentException(
            "Unsupported container: $container. Kohii only supports Fragment, FragmentActivity."
        )
      }
    val managerGroup = groups.getOrPut(activity) {
      val result = PlaybackManagerGroup(this, activity)
      activity.lifecycle.addObserver(result)
      return@getOrPut result
    }
    val manager = managers.getOrPut(managerLifecycleOwner) {
      // Create new in case of no cache found.
      require(managerLifecycleOwner !is Service) { "Service is not supported yet." }
      return@getOrPut ViewPlaybackManager(this, container, managerGroup, managerLifecycleOwner)
    }

    hosts.asSequence() // Will drop the null TargetHost.
        .mapNotNull {
          if (it is TargetHost) it // TODO why do we need this?
          else TargetHost.createTargetHost(it, manager)
        }
        .forEach {
          manager.registerTargetHost(it)
        }

    managerLifecycleOwner.lifecycle.addObserver(manager)
    managerGroup.attachPlaybackManager(manager)
    return manager
  }

  @ExoPlayer
  fun setUp(uri: Uri) = this.setUp(MediaItem(uri))

  @ExoPlayer
  fun setUp(url: String) = this.setUp(url.toUri())

  @ExoPlayer
  fun setUp(media: Media) = defaultPlayableCreator.setUp(media)

  @ExoPlayer
  @Beta(message = "Helper method to help build MediaSource from a Media instance.")
  fun createMediaSource(media: Media): MediaSource {
    return defaultBridgeProvider.createMediaSource(media)
  }

  /**
   * Manually pause an object in a specific scope. After Playbacks of a Scope is paused by this method,
   * only another call to [#resume(receiver, scope)] with same or higher priority Scope will resume it.
   * For example:
   * - Pausing a Playback with Scope.PLAYBACK --> a call to resume(playback, Scope.PLAYBACK) or
   * resume(playback, Scope.HOST) will also resume it.
   * - Pausing all Playback in a Manager by using Scope.MANAGER --> a call to resume(targetHost, Scope.HOST)
   * will not resume anything, including Playbacks mapped to that targetHost.
   *
   * To be able to change the scope of Playbacks need to be paused, client must:
   * - Resume all Playbacks of the same or higher priority Scope.
   * - Call this method to pause Playbacks of expected scope.
   */
  fun pause(
    receiver: Any?,
    scope: Scope
  ) {
    when {
      scope === Scope.GLOBAL ->
        // receiver is ignored.
        this.groups.forEach {
          this.pause(it.value, Scope.ACTIVITY)
        }
      scope === Scope.ACTIVITY ->
        when (receiver) {
          is PlaybackManagerGroup -> {
            receiver.lock.set(true)
            receiver.managers()
                .forEach { this.pause(it, Scope.MANAGER) }
          }
          is PlaybackManager -> this.pause(receiver.parent, Scope.ACTIVITY)
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a PlaybackManager or a PlaybackManagerGroup"
          )
        }
      scope === Scope.MANAGER ->
        (receiver as? PlaybackManager)?.let {
          it.lock.set(true)
          it.targetHosts.forEach { host -> this.pause(host, Scope.HOST) }
        } ?: throw IllegalArgumentException("Receiver for scope $scope must be a PlaybackManager")
      scope === Scope.HOST ->
        when (receiver) {
          is TargetHost -> {
            receiver.lock = true
            receiver.manager.dispatchRefreshAll()
          }
          is Playback<*> -> this.pause(receiver.targetHost, Scope.HOST)
          else -> {
            val host = this.managers.values.asSequence()
                .map { it.targetHosts.find { targetHost -> targetHost.host === receiver } }
                .filterNotNull()
                .firstOrNull()

            if (host != null) this.pause(host, Scope.HOST)
          }
        }
      scope === Scope.PLAYBACK ->
        (receiver as? Playback<*>)?.let { this.pause(it) }
            ?: throw IllegalArgumentException("Receiver for scope $scope must be a Playback")
    }
  }

  /**
   * Manually resume all Playback of a Scope those are paused by [#pause(receiver, Scope)].
   */
  fun resume(
    receiver: Any?,
    scope: Scope
  ) {
    when {
      scope === Scope.GLOBAL ->
        this.groups.forEach {
          this.resume(it.value, Scope.ACTIVITY)
        }
      scope === Scope.ACTIVITY ->
        when (receiver) {
          is PlaybackManagerGroup -> {
            receiver.lock.set(false)
            receiver.managers()
                .forEach { this.resume(it, Scope.MANAGER) }
          }
          is PlaybackManager -> this.resume(receiver.parent, Scope.ACTIVITY)
          else -> throw IllegalArgumentException(
              "Receiver for scope $scope must be a PlaybackManager or a PlaybackManagerGroup"
          )
        }
      scope === Scope.MANAGER ->
        (receiver as? PlaybackManager)?.let {
          it.lock.set(false)
          it.targetHosts.forEach { host -> this.resume(host, Scope.HOST) }
        } ?: throw IllegalArgumentException("Receiver for scope $scope must be a PlaybackManager")
      scope === Scope.HOST ->
        when (receiver) {
          is TargetHost -> {
            receiver.lock = false
            receiver.manager.dispatchRefreshAll()
          }
          is Playback<*> -> this.resume(receiver.targetHost, Scope.HOST)
          else -> {
            // Find the TargetHost whose host is this receiver
            val host = this.managers.values.asSequence()
                .map { it.targetHosts.find { targetHost -> targetHost.host === receiver } }
                .filterNotNull()
                .firstOrNull()

            if (host != null) this.resume(host, Scope.HOST)
          }
        }
      scope === Scope.PLAYBACK ->
        (receiver as? Playback<*>)?.let {
          if (it.controller != null) { // has manual controller
            manualPlayableRecord.remove(it.playable) // TODO consider to not do this?
            it.manager.dispatchRefreshAll()
          }
        } ?: throw IllegalArgumentException("Receiver for scope $scope must be a Playback")
    }
  }

  fun applyVolumeInfo(
    volumeInfo: VolumeInfo,
    receiver: Any?,
    scope: Scope
  ) {
    when (receiver) {
      is Playback<*> -> receiver.manager.applyVolumeInfo(volumeInfo, receiver, scope)
      is TargetHost -> receiver.manager.applyVolumeInfo(volumeInfo, receiver, scope)
      is PlaybackManager -> receiver.applyVolumeInfo(volumeInfo, receiver, scope)
      is PlaybackManagerGroup -> receiver.managers().forEach {
        it.applyVolumeInfo(volumeInfo, receiver, scope)
      }
      else -> throw IllegalArgumentException("Unsupported receiver: $receiver")
    }
  }

  fun fetchRebinder(tag: Any?): Rebinder<*>? {
    val cache = if (tag is String) this.mapTagToPlayable[tag] else null
    return if (cache != null) {
      Rebinder(tag as String, cache.second)
    } else null
  }

  fun promote(playback: Playback<*>) {
    // 1. Promote the Host
    val manager = playback.manager
    manager.promote(playback.targetHost)
    // 2. Promote the Manager
    manager.parent.promote(manager)
    manager.dispatchRefreshAll()
  }

  // [END] Public API
}
