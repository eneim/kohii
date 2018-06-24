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

package kohii.v1;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import kohii.media.PlaybackInfo;
import kohii.media.VolumeInfo;

/**
 * @author eneim (2018/06/24).
 */
public interface Helper {

  /**
   * Prepare the resource for a {@link ExoPlayer}. This method should:
   * - Request for new {@link ExoPlayer} instance if there is not a usable one.
   * - Configure {@link PlayerEventListener} for it.
   * - If there is non-trivial PlaybackInfo, update it to the SimpleExoPlayer.
   * - If client request to prepare MediaSource, then prepare it.
   *
   * This method must be called before {@link #setPlayerView(PlayerView)}.
   *
   * @param loadSource if {@code true}, also prepare the MediaSource when preparing the Player,
   * if {@code false} just do nothing for the MediaSource.
   */
  void prepare(boolean loadSource);

  /**
   * Set the {@link PlayerView} for this Playable. It is expected that a playback doesn't require a
   * UI, so this setup is optional. But it must be called after the SimpleExoPlayer is prepared,
   * that is after {@link #prepare(boolean)} and before {@link #release()}.
   *
   * Changing the PlayerView during playback is expected, though not always recommended, especially
   * on old Devices with low Android API.
   *
   * @param playerView the PlayerView to set to the SimpleExoPlayer.
   */
  void setPlayerView(@Nullable PlayerView playerView);

  /**
   * Get current {@link PlayerView} of this Playable.
   *
   * @return current PlayerView instance of this Playable.
   */
  @Nullable PlayerView getPlayerView();

  /**
   * Start the playback. If the {@link MediaSource} is not prepared, then also prepare it.
   */
  void play();

  /**
   * Pause the playback.
   */
  void pause();

  /**
   * Reset all resource, so that the playback can start all over again. This is to cleanup the
   * playback for reuse. The SimpleExoPlayer instance must be still usable without calling
   * {@link #prepare(boolean)}.
   */
  void reset();

  /**
   * Release all resource. After this, the SimpleExoPlayer is released to the Player pool and the
   * Playable must call {@link #prepare(boolean)} again to use it again.
   */
  void release();

  /**
   * Get current {@link PlaybackInfo} of the playback.
   *
   * @return current PlaybackInfo of the playback.
   */
  @NonNull PlaybackInfo getPlaybackInfo();

  /**
   * Set the custom {@link PlaybackInfo} for this playback. This could suggest a seek.
   *
   * @param playbackInfo the PlaybackInfo to set for this playback.
   */
  void setPlaybackInfo(@NonNull PlaybackInfo playbackInfo);

  /**
   * Add a new {@link PlayerEventListener} to this Playable. As calling {@link #prepare(boolean)}
   * also
   * triggers some internal events, this method should be called before {@link #prepare(boolean)}
   * so
   * that Client could received them all.
   *
   * @param listener the EventListener to add, must be not {@code null}.
   */
  void addEventListener(@NonNull PlayerEventListener listener);

  /**
   * Remove an {@link PlayerEventListener} from this Playable.
   *
   * @param listener the EventListener to be removed. If null, nothing happens.
   */
  void removeEventListener(PlayerEventListener listener);

  /**
   * !This must only work if the Player in use is a {@link ToroExoPlayer}.
   */
  void addOnVolumeChangeListener(@NonNull VolumeChangeListener listener);

  void removeOnVolumeChangeListener(@Nullable VolumeChangeListener listener);

  /**
   * Check if current Playable is playing or not.
   *
   * @return {@code true} if this Playable is playing, {@code false} otherwise.
   */
  boolean isPlaying();

  /**
   * Update playback's volume.
   *
   * @param volumeInfo the {@link VolumeInfo} to update to.
   * @return {@code true} if current Volume info is updated, {@code false} otherwise.
   */
  boolean setVolumeInfo(@NonNull VolumeInfo volumeInfo);

  /**
   * Get current {@link VolumeInfo}.
   */
  @NonNull VolumeInfo getVolumeInfo();

  /**
   * Same as {@link Player#setPlaybackParameters(PlaybackParameters)}
   */
  void setParameters(@Nullable PlaybackParameters parameters);

  /**
   * Same as {@link Player#getPlaybackParameters()}
   */
  @Nullable PlaybackParameters getParameters();

  void setRepeatMode(@Player.RepeatMode int repeatMode);

  @Player.RepeatMode int getRepeatMode();
}
