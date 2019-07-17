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

package kohii.v1.exo

import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector

// Set of default ExoPlayer configuration
object DefaultExoPlayerConfig {

  /** [Player.setPlaybackParameters] */
  val PLAYBACK_PARAMS = PlaybackParameters.DEFAULT

  /** [DefaultTrackSelector.setParameters] */
  val SELECTOR_PARAMS = DefaultTrackSelector.Parameters.DEFAULT

  /** [Player.setRepeatMode] */
  val REPEAT_MODE = Player.REPEAT_MODE_OFF

  /** [Player.setShuffleModeEnabled] */
  val SHUFFLE_ENABLED = false

  /** [Player.AudioComponent.setAudioAttributes] */
  val AUDIO_ATTRIBUTES = AudioAttributes.DEFAULT
}
