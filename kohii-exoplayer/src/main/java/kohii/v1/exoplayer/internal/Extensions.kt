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

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.AudioComponent
import kohii.v1.core.PlayerEventListener
import kohii.v1.core.VolumeInfoController
import kohii.v1.media.VolumeInfo

fun Player.getVolumeInfo(): VolumeInfo {
  return when (this) {
    is VolumeInfoController -> VolumeInfo(this.volumeInfo)
    is AudioComponent -> { // will match SimpleExoPlayer
      val volume = this.volume
      VolumeInfo(volume == 0f, volume)
    }
    else -> throw UnsupportedOperationException(javaClass.name + " doesn't support this.")
  }
}

fun Player.setVolumeInfo(volume: VolumeInfo) {
  when (this) {
    is VolumeInfoController -> this.setVolumeInfo(volume)
    is AudioComponent -> { // will match SimpleExoPlayer
      if (volume.mute) {
        this.volume = 0f
      } else {
        this.volume = volume.volume
      }
      this.setAudioAttributes(this.audioAttributes, !volume.mute)
    }
    else -> throw UnsupportedOperationException(javaClass.name + " doesn't support this.")
  }
}

fun Player.addEventListener(listener: PlayerEventListener) {
  this.addListener(listener)
  this.videoComponent?.addVideoListener(listener)
  this.audioComponent?.addAudioListener(listener)
  this.textComponent?.addTextOutput(listener)
  this.metadataComponent?.addMetadataOutput(listener)
}

fun Player.removeEventListener(listener: PlayerEventListener) {
  this.removeListener(listener)
  this.videoComponent?.removeVideoListener(listener)
  this.audioComponent?.removeAudioListener(listener)
  this.textComponent?.removeTextOutput(listener)
  this.metadataComponent?.removeMetadataOutput(listener)
}
