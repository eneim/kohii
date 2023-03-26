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

package kohii.v1.exoplayer.internal

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import kohii.v1.core.VolumeInfoController
import kohii.v1.media.VolumeInfo

fun Player.getVolumeInfo(): VolumeInfo {
  return when (this) {
    is VolumeInfoController -> VolumeInfo(this.volumeInfo)
    else -> {
      val volume = this.volume
      VolumeInfo(volume == 0f, volume)
    }
  }
}

fun Player.setVolumeInfo(volume: VolumeInfo) {
  when (this) {
    is VolumeInfoController -> this.setVolumeInfo(volume)
    else -> {
      if (volume.mute) {
        this.volume = 0f
      } else {
        this.volume = volume.volume
      }
      if (this is ExoPlayer) {
        val audioAttributes = this.audioAttributes
        this.setAudioAttributes(audioAttributes, !volume.mute)
      }
    }
  }
}
