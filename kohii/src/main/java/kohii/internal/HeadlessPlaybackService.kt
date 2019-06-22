/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
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

package kohii.internal

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.LifecycleService
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener
import kohii.v1.HeadlessPlayback
import kohii.v1.HeadlessPlaybackParams
import kohii.v1.Kohii
import kohii.v1.Playable
import kohii.v1.exo.PlayerViewBridge
import kohii.v1.exo.PlayerViewPlayable

// TODO support Bitmap.
internal class HeadlessPlaybackService : LifecycleService(), HeadlessPlayback {

  companion object {
    const val KEY_PARAMS = "kohii::v1::service::params"
    const val KEY_PLAYABLE = "kohii::v1::service::playable"
  }

  lateinit var kohii: Kohii
  lateinit var playable: Playable<*>

  private var playerNotificationManager: PlayerNotificationManager? = null

  override fun onCreate() {
    super.onCreate()
    kohii = Kohii[this]
    kohii.setHeadlessPlayback(this)
  }

  override fun onStartCommand(
    intent: Intent,
    flags: Int,
    startId: Int
  ): Int {
    val extras = intent.extras ?: throw IllegalArgumentException("Service has no extras.")
    // Get the Playable.
    val playableTag = extras.getString(KEY_PLAYABLE)
        ?: throw IllegalArgumentException("No playable tag provided.")
    val cache = kohii.mapTagToPlayable[playableTag]
    if (cache == null) {
      stopSelf()
      return super.onStartCommand(intent, flags, startId)
    }

    val params = extras.getParcelable(KEY_PARAMS) as HeadlessPlaybackParams

    val playable = cache.first
    this.playable = playable
    if (playable is PlayerViewPlayable) {
      val bridge = playable.bridge as PlayerViewBridge
      playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
          this,
          params.channelId,
          params.channelName,
          params.notificationId,
          object : MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player?): PendingIntent? {
              return null
            }

            override fun getCurrentContentText(player: Player?): String? {
              return params.mediaText
            }

            override fun getCurrentContentTitle(player: Player?): String {
              return params.mediaTitle
            }

            override fun getCurrentLargeIcon(
              player: Player?,
              callback: BitmapCallback?
            ): Bitmap? {
              return null
            }
          }
      )

      playerNotificationManager?.setNotificationListener(object : NotificationListener {
        override fun onNotificationCancelled(notificationId: Int) {
          stopSelf()
        }

        override fun onNotificationStarted(
          notificationId: Int,
          notification: Notification?
        ) {
          startForeground(notificationId, notification)
        }
      })

      playerNotificationManager?.setPlayer(bridge.player)
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (::playable.isInitialized) {
      if (kohii.mapPlayableToManager[playable] == kohii) {
        kohii.mapPlayableToManager.remove(playable)
      }

      if (kohii.mapPlayableToManager[playable] == null) {
        playable.release()
        kohii.releasePlayable(playable.tag, playable)
      }
    }
    playerNotificationManager?.setPlayer(null)
    if (kohii.shouldCleanUp()) {
      kohii.cleanUp()
    }
    kohii.setHeadlessPlayback(null)
  }

  override fun dismiss() {
    stopSelf()
  }
}
