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

package kohii.v1.core

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import com.google.android.exoplayer2.ExoPlayerLibraryInfo

object Common {

  const val REPEAT_MODE_OFF = 0 // Player.REPEAT_MODE_OFF
  const val REPEAT_MODE_ONE = 1 // Player.REPEAT_MODE_ONE
  const val REPEAT_MODE_ALL = 2 // Player.REPEAT_MODE_ALL
  const val REPEAT_MODE_GROUP = 3

  const val STATE_IDLE = 1 // Player.STATE_IDLE
  const val STATE_BUFFERING = 2 // Player.STATE_BUFFERING
  const val STATE_READY = 3 // Player.STATE_READY
  const val STATE_ENDED = 4 // Player.STATE_ENDED

  internal val PLAY = PlaybackAction(true)
  internal val PAUSE = PlaybackAction(false)

  // ExoPlayer's doesn't catch a RuntimeException and crash if Device has too many App installed.
  @RestrictTo(LIBRARY_GROUP_PREFIX)
  fun getUserAgent(
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

    return "$appName/$versionName (Linux;Android ${Build.VERSION.RELEASE}) ${ExoPlayerLibraryInfo.VERSION_SLASHY}"
  }
}
