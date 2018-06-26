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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.widget.Toast;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import kohii.media.MediaDrm;
import kohii.v1.R;

import static android.widget.Toast.LENGTH_SHORT;
import static com.google.android.exoplayer2.drm.UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME;
import static com.google.android.exoplayer2.util.Util.getDrmUuid;

/**
 * @author eneim (2018/06/25).
 */
public class DefaultDrmSessionManagerFactory implements DrmSessionManagerFactory {

  private static final Map<MediaDrm, DrmSessionManager<FrameworkMediaCrypto>> cache =
      new TreeMap<>(new Comparator<MediaDrm>() {
        @Override public int compare(MediaDrm o1, MediaDrm o2) {
          return o1.compareTo(o2);
        }
      });

  private final ExoStore store;

  DefaultDrmSessionManagerFactory(ExoStore store) {
    this.store = store;
  }

  @Override public DrmSessionManager<FrameworkMediaCrypto> createDrmSessionManager(
      @Nullable MediaDrm mediaDrm) {
    if (mediaDrm == null) return null;
    DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = cache.get(mediaDrm);
    if (drmSessionManager != null) return drmSessionManager;

    int errorStringId = R.string.error_drm_unknown;
    String subString = null;
    if (Util.SDK_INT < 18) {
      errorStringId = R.string.error_drm_not_supported;
    } else {
      UUID drmSchemeUuid = getDrmUuid(mediaDrm.getType());
      if (drmSchemeUuid == null) {
        errorStringId = R.string.error_drm_unsupported_scheme;
      } else {
        HttpDataSource.Factory factory = new DefaultHttpDataSourceFactory(store.appName);
        try {
          drmSessionManager = buildDrmSessionManagerV18(drmSchemeUuid, mediaDrm.getLicenseUrl(),
              mediaDrm.getKeyRequestPropertiesArray(), mediaDrm.multiSession(), factory);
        } catch (UnsupportedDrmException e) {
          e.printStackTrace();
          errorStringId = e.reason == REASON_UNSUPPORTED_SCHEME ? //
              R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
          if (e.reason == REASON_UNSUPPORTED_SCHEME) {
            subString = mediaDrm.getType();
          }
        }
      }
    }

    if (drmSessionManager == null) {
      String error = TextUtils.isEmpty(subString) ? store.context.getString(errorStringId)
          : store.context.getString(errorStringId) + ": " + subString;
      Toast.makeText(store.context, error, LENGTH_SHORT).show();
    }

    cache.put(mediaDrm, drmSessionManager);
    return drmSessionManager;
  }

  @RequiresApi(18) private static DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
      @NonNull UUID uuid, @Nullable String licenseUrl, @Nullable String[] keyRequestPropertiesArray,
      boolean multiSession, @NonNull HttpDataSource.Factory httpDataSourceFactory)
      throws UnsupportedDrmException {
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, httpDataSourceFactory);
    if (keyRequestPropertiesArray != null) {
      for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
        drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
            keyRequestPropertiesArray[i + 1]);
      }
    }
    return new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback,
        null, multiSession);
  }
}
