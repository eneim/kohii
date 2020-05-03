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

import kohii.v1.media.PlaybackInfo
import kohii.v1.media.VolumeInfo

interface Bridge<RENDERER : Any> {

  val playerState: Int

  var renderer: RENDERER?

  @Deprecated("From 1.1.0+ this value should not be used anymore.")
  var videoSize: VideoSize

  var playbackInfo: PlaybackInfo

  // var parameters: PlaybackParameters

  var repeatMode: Int

  var volumeInfo: VolumeInfo

  var playerParameters: PlayerParameters

  fun isPlaying(): Boolean

  fun seekTo(positionMs: Long)

  /**
   * Prepare the resource for a media. This method should:
   * - Request for new Player instance if there is not a usable one.
   * - Configure callbacks for the player implementation.
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
  fun ready()

  fun play()

  fun pause()

  /**
   * Reset all resource, so that the playback can start all over again. This is to cleanup the
   * playback for reuse. The ExoPlayer instance must be still usable without calling [prepare].
   */
  fun reset(resetPlayer: Boolean = true)

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
   * Remove a [PlayerEventListener] from this Playable.
   *
   * @param listener the EventListener to be removed. If null, nothing happens.
   */
  fun removeEventListener(listener: PlayerEventListener?)

  fun addVolumeChangeListener(listener: VolumeChangedListener)

  fun removeVolumeChangeListener(listener: VolumeChangedListener?)

  fun addErrorListener(errorListener: ErrorListener)

  fun removeErrorListener(errorListener: ErrorListener?)
}
