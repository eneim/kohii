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

package kohii.v1

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Playable.RepeatMode

interface Bridge<OUTPUT : Any> {

  //  set/get
  var playerView: OUTPUT?

  // set/get
  var playbackInfo: PlaybackInfo

  // set/get
  var parameters: PlaybackParameters

  @RepeatMode var repeatMode: Int

  val isPlaying: Boolean

  val volumeInfo: VolumeInfo

  fun seekTo(positionMs: Long)

  /**
   * Prepare the resource for a [ExoPlayer]. This method should:
   * - Request for new [ExoPlayer] instance if there is not a usable one.
   * - Configure [PlayerEventListener] for it.
   * - If there is non-trivial PlaybackInfo, update it to the SimpleExoPlayer.
   * - If client request to prepare MediaSource, then prepare it.
   *
   * This method must be called before [.setPlayerView].
   *
   * @param loadSource if `true`, also prepare the MediaSource when preparing the Player,
   * if `false` just do nothing for the MediaSource.
   */
  fun prepare(loadSource: Boolean)

  // Ensure resource is ready to play. PlaybackDispatcher will require this for manual playback.
  fun ensurePreparation()

  /** [com.google.android.exoplayer2.Player.setPlayWhenReady] to true */
  fun play()

  /** [com.google.android.exoplayer2.Player.setPlayWhenReady] to false */
  fun pause()

  /**
   * Reset all resource, so that the playback can start all over again. This is to cleanup the
   * playback for reuse. The ExoPlayer instance must be still usable without calling [prepare].
   */
  fun reset()

  /**
   * Release all resource. After this, the Player instance is released to the Player pool and the
   * Bridge must call [prepare] to request for a Player it again.
   */
  fun release()

  /**
   * Add a new [PlayerEventListener] to this Playable. As calling [prepare] also triggers some
   * internal events, this method should be called before [prepare] so that Client could received
   * them all.
   *
   * @param listener the EventListener to add, must be not `null`.
   */
  fun addEventListener(listener: PlayerEventListener)

  /**
   * Remove an [PlayerEventListener] from this Playable.
   *
   * @param listener the EventListener to be removed. If null, nothing happens.
   */
  fun removeEventListener(listener: PlayerEventListener?)

  fun addVolumeChangeListener(listener: VolumeChangedListener)

  fun removeVolumeChangeListener(listener: VolumeChangedListener?)

  fun addErrorListener(errorListener: ErrorListener)

  fun removeErrorListener(errorListener: ErrorListener?)

  /**
   * Update playback's volume.
   *
   * @param volumeInfo the [VolumeInfo] to update to.
   * @return `true` if current Volume info is updated, `false` otherwise.
   */
  fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean
}
