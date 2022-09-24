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
import com.google.android.exoplayer2.Player

object Common {

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.REPEAT_MODE_OFF",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.REPEAT_MODE_ONE",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.REPEAT_MODE_ALL",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.REPEAT_MODE_ALL",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val REPEAT_MODE_GROUP = Player.REPEAT_MODE_ALL

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.STATE_IDLE",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val STATE_IDLE = Player.STATE_IDLE

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.STATE_BUFFERING",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val STATE_BUFFERING = Player.STATE_BUFFERING

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.STATE_READY",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val STATE_READY = Player.STATE_READY

  @Deprecated(
      message = "Use constant from Player class directly instead.",
      replaceWith = ReplaceWith(
          expression = "Player.STATE_ENDED",
          imports = ["com.google.android.exoplayer2.Player"]
      )
  )
  const val STATE_ENDED = Player.STATE_ENDED

  internal const val PLAY = true
  internal const val PAUSE = false

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
