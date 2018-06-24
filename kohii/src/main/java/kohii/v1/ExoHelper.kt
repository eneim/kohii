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

import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo

/**
 * @author eneim (2018/06/24).
 */
class ExoHelper(): Helper {

  override fun prepare(loadSource: Boolean) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun setPlayerView(playerView: PlayerView?) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getPlayerView(): PlayerView? {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun play() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun pause() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun reset() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun release() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getPlaybackInfo(): PlaybackInfo {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun setPlaybackInfo(playbackInfo: PlaybackInfo) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun addEventListener(listener: PlayerEventListener) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun removeEventListener(listener: PlayerEventListener?) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun addOnVolumeChangeListener(listener: VolumeChangeListener) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun removeOnVolumeChangeListener(listener: VolumeChangeListener?) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isPlaying(): Boolean {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getVolumeInfo(): VolumeInfo {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun setParameters(parameters: PlaybackParameters?) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getParameters(): PlaybackParameters? {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun setRepeatMode(repeatMode: Int) {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getRepeatMode(): Int {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}