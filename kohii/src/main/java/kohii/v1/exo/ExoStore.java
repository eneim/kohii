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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.util.Pools;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.util.Util;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;
import kohii.media.VolumeInfo;

import static com.google.android.exoplayer2.util.Util.getUserAgent;
import static java.lang.Runtime.getRuntime;
import static kohii.v1.BuildConfig.LIB_NAME;

/**
 * Store for {@link ExoPlayer}s
 *
 * @author eneim (2018/06/24).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) public class ExoStore {

  // Magic number: Build.VERSION.SDK_INT / 6 --> API 16 ~ 18 will set pool size to 2, etc.
  @SuppressWarnings("WeakerAccess") //
  static final int MAX_POOL_SIZE = Math.max(Util.SDK_INT / 6, getRuntime().availableProcessors());

  @SuppressLint("StaticFieldLeak") private static ExoStore exoStore;

  public static ExoStore get(Context context) {
    if (exoStore == null) {
      synchronized (ExoStore.class) {
        if (exoStore == null) exoStore = new ExoStore(context);
      }
    }

    return exoStore;
  }

  @NonNull final String appName;
  @NonNull final Context context;  // Application context
  @NonNull private final Map<Config, PlayerFactory> factories;
  @NonNull private final Map<PlayerFactory, Pools.Pool<Player>> playerPools;
  private final Map<Config, Pools.Pool<Player>> mapConfigToPool;

  ExoStore(@NonNull Context context) {
    this.context = context.getApplicationContext();
    this.appName = getUserAgent(context, LIB_NAME);
    this.playerPools = new HashMap<>();
    this.factories = new HashMap<>();

    this.mapConfigToPool = new HashMap<>();

    // Adapt from ExoPlayer demo app. Start this on demand.
    CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    if (CookieHandler.getDefault() != cookieManager) {
      CookieHandler.setDefault(cookieManager);
    }
  }

  private Pools.Pool<Player> getPool(PlayerFactory creator) {
    Pools.Pool<Player> pool = playerPools.get(creator);
    if (pool == null) {
      pool = new Pools.SimplePool<>(MAX_POOL_SIZE);
      playerPools.put(creator, pool);
    }

    return pool;
  }

  private Pools.Pool<Player> getPool(Config config) {
    Pools.Pool<Player> pool = mapConfigToPool.get(config);
    if (pool == null) {
      pool = new Pools.SimplePool<>(MAX_POOL_SIZE);
      mapConfigToPool.put(config, pool);
    }

    return pool;
  }

  public final Player acquirePlayer(Config config) {
    Player player = getPool(config).acquire();
    if (player == null) player = new DefaultPlayerFactory(this, config).createPlayer();
    return player;
  }

  public final void releasePlayer(@NonNull Player player, @NonNull Config config) {
    PlayerFactory creator = this.factories.get(config);
    getPool(creator).release(player);
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) //
  public static void setVolumeInfo(@NonNull Player player, @NonNull VolumeInfo volume) {
    if (player instanceof KohiiPlayer) {
      ((KohiiPlayer) player).setVolumeInfo(volume);
    } else if (player instanceof SimpleExoPlayer) {
      if (volume.getMute()) {
        ((SimpleExoPlayer) player).setVolume(0f);
      } else {
        ((SimpleExoPlayer) player).setVolume(volume.getVolume());
      }
    } else {
      throw new RuntimeException(player.getClass().getSimpleName() + " doesn't support this.");
    }
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) //
  public static VolumeInfo getVolumeInfo(Player player) {
    if (player instanceof KohiiPlayer) {
      return new VolumeInfo(((KohiiPlayer) player).getVolumeInfo());
    } else if (player instanceof SimpleExoPlayer) {
      float volume = ((SimpleExoPlayer) player).getVolume();
      return new VolumeInfo(volume == 0, volume);
    } else {
      throw new RuntimeException(player.getClass().getSimpleName() + " doesn't support this.");
    }
  }
}
