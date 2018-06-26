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
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import kohii.v1.Playable;

import static android.text.TextUtils.isEmpty;
import static com.google.android.exoplayer2.util.Util.inferContentType;

/**
 * @author eneim (2018/06/25).
 */
public class DefaultMediaSourceFactory implements MediaSourceFactory {

  private final ExoStore store;

  public DefaultMediaSourceFactory(ExoStore store) {
    this.store = store;
  }

  @NonNull @Override public MediaSource createMediaSource(Playable.Options options) {
    @C.ContentType int type = isEmpty(options.getMediaType()) ? inferContentType(options.getUri())
        : inferContentType("." + options.getMediaType());
    TransferListener<? super DataSource> transferListener = options.getConfig().getMeter();
    Cache cache = options.getConfig().getCache();

    DataSource.Factory mediaDataSourceFactory =
        new DefaultDataSourceFactory(store.context, store.appName, transferListener);
    if (cache != null) {
      mediaDataSourceFactory = new CacheDataSourceFactory(cache, mediaDataSourceFactory);
    }

    DataSource.Factory manifestDataSourceFactory =
        new DefaultDataSourceFactory(store.context, store.appName);

    MediaSource mediaSource;
    switch (type) {
      case C.TYPE_SS:
        mediaSource =
            new SsMediaSource.Factory(new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory).createMediaSource(options.getUri());
        break;
      case C.TYPE_DASH:
        mediaSource =
            new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory).createMediaSource(options.getUri());
        break;
      case C.TYPE_HLS:
        mediaSource =
            new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(options.getUri());
        break;
      case C.TYPE_OTHER:
        mediaSource = new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(
            options.getUri());
        break;
      default:
        throw new IllegalStateException("Unsupported type: " + type);
    }

    return mediaSource;
  }
}
