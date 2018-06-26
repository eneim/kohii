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

package kohii.v1.exo;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import java.util.HashSet;
import java.util.Set;
import kohii.media.VolumeInfo;
import kohii.v1.OnVolumeChangedListener;

/**
 * @author eneim (2018/06/25).
 */
@SuppressWarnings("WeakerAccess") //
public class KohiiPlayer extends SimpleExoPlayer {

  protected KohiiPlayer(RenderersFactory renderersFactory, TrackSelector trackSelector,
      LoadControl loadControl,
      @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
    super(renderersFactory, trackSelector, loadControl, drmSessionManager);
  }

  private Set<OnVolumeChangedListener> volumeChangedListeners;
  private final VolumeInfo volumeInfo = new VolumeInfo(false, 1f);

  @CallSuper @Override public void setVolume(float audioVolume) {
    this.setVolumeInfo(new VolumeInfo(audioVolume == 0, audioVolume));
  }

  @SuppressWarnings("UnusedReturnValue")
  public final boolean setVolumeInfo(@NonNull VolumeInfo volumeInfo) {
    boolean changed = !this.volumeInfo.equals(volumeInfo);
    if (changed) {
      this.volumeInfo.setTo(volumeInfo.getMute(), volumeInfo.getVolume());
      super.setVolume(volumeInfo.getMute() ? 0 : volumeInfo.getVolume());
      if (volumeChangedListeners != null) {
        for (OnVolumeChangedListener listener : this.volumeChangedListeners) {
          listener.onVolumeChanged(volumeInfo);
        }
      }
    }

    return changed;
  }

  @SuppressWarnings("unused") //
  @NonNull public final VolumeInfo getVolumeInfo() {
    return volumeInfo;
  }

  public final void addOnVolumeChangedListener(@NonNull OnVolumeChangedListener listener) {
    if (volumeChangedListeners == null) volumeChangedListeners = new HashSet<>();
    volumeChangedListeners.add(listener);
  }

  public final void removeOnVolumeChangedListener(OnVolumeChangedListener listener) {
    if (volumeChangedListeners != null) volumeChangedListeners.remove(listener);
  }

  public final void clearOnVolumeChangedListener() {
    if (volumeChangedListeners != null) volumeChangedListeners.clear();
  }
}
