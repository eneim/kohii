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

package kohii.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import kohii.Draft;
import kohii.Experiment;

/**
 * @author eneim (2018/07/27).
 */
@SuppressWarnings("unused") @Draft @Experiment  //
public final class AudioStateManager {

  @SuppressLint("StaticFieldLeak") private static AudioStateManager instance;

  public static AudioStateManager getInstance(Context context) {
    if (instance == null) {
      instance = new AudioStateManager(context);
    }

    return instance;
  }

  private final AudioManager audioManager;

  private AudioStateManager(Context context) {
    this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  public final boolean requestAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return requestAudioFocusV26();
    } else {
      return requestAudioFocusBase();
    }
  }

  public final void abandonAudioFocus() {
    if (audioManager == null) return;
    audioManager.abandonAudioFocus(null);
  }

  @RequiresApi(api = Build.VERSION_CODES.O) //
  private boolean requestAudioFocusV26() {
    if (audioManager == null) return false;

    AudioAttributes audioAttributes =
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build();

    AudioFocusRequest audioFocusRequest = //
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN) //
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
              @Override public void onAudioFocusChange(int focus) {
                switch (focus) {
                  case AudioManager.AUDIOFOCUS_LOSS:
                    // Stop and release your player
                    break;
                  case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Pause your player
                    break;
                  case AudioManager.AUDIOFOCUS_GAIN:
                    // Restore volume and resume player if needed
                    break;
                  case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Lower volume
                    break;
                }
              }
            })
            .build();
    int focusRequest = audioManager.requestAudioFocus(audioFocusRequest);
    switch (focusRequest) {
      case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
        // don’t start playback
      case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
        // actually start playback
    }

    return true;
  }

  private boolean requestAudioFocusBase() {
    if (audioManager == null) return false;

    int focusRequest = audioManager.requestAudioFocus(  //
        new AudioManager.OnAudioFocusChangeListener() {
          @Override public void onAudioFocusChange(int focus) {
            switch (focus) {
              case AudioManager.AUDIOFOCUS_LOSS:
                // stop and release your player
                break;
              case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // pause your player
                break;
              case AudioManager.AUDIOFOCUS_GAIN:
                // restore volume and resume player if needed
                break;
              case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lower volume
                break;
            }
          }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

    switch (focusRequest) {
      case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
        // don't start playback
      case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
        // actually start playback
    }

    return true;
  }
}
