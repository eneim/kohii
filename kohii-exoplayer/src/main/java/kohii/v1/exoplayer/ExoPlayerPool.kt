/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.exoplayer

import android.content.Context
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.AudioComponent
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util
import kohii.v1.core.PlayerPool
import kohii.v1.media.Media

/**
 * A [PlayerPool] for the [Player] implementation. By default it uses the [KohiiExoPlayer]
 * implementation.
 *
 * @see [KohiiExoPlayer]
 */
class ExoPlayerPool(
  poolSize: Int = DEFAULT_POOL_SIZE,
  private val context: Context,
  private val clock: Clock = Clock.DEFAULT,
  private val bandwidthMeterFactory: BandwidthMeterFactory = ExoPlayerConfig.DEFAULT,
  private val trackSelectorFactory: TrackSelectorFactory = ExoPlayerConfig.DEFAULT,
  private val loadControlFactory: LoadControlFactory = ExoPlayerConfig.DEFAULT,
  private val renderersFactory: RenderersFactory =
    DefaultRenderersFactory(context.applicationContext)
) : PlayerPool<Player>(poolSize) {

  override fun createPlayer(media: Media): Player = KohiiExoPlayer(
      context.applicationContext,
      clock,
      renderersFactory,
      trackSelectorFactory.createDefaultTrackSelector(context.applicationContext),
      loadControlFactory.createLoadControl(),
      bandwidthMeterFactory.createBandwidthMeter(context.applicationContext),
      Util.getLooper()
  )

  override fun resetPlayer(player: Player) {
    super.resetPlayer(player)
    player.stop(true)
    if (player is AudioComponent) {
      player.setAudioAttributes(AudioAttributes.DEFAULT, true)
    }
  }

  override fun destroyPlayer(player: Player) = player.release()
}
