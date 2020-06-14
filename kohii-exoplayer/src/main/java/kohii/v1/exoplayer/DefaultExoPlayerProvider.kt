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
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util

/**
 * @author eneim (2018/10/27).
 */
class DefaultExoPlayerProvider @JvmOverloads constructor(
  context: Context,
  private val clock: Clock = Clock.DEFAULT,
  private val bandwidthMeterFactory: BandwidthMeterFactory = DefaultBandwidthMeterFactory(),
  private val trackSelectorFactory: TrackSelectorFactory = DefaultTrackSelectorFactory(),
  private val loadControl: LoadControl = DefaultLoadControl(),
  private val renderersFactory: RenderersFactory =
    DefaultRenderersFactory(context.applicationContext)
) : RecycledExoPlayerProvider(context) {

  override fun createExoPlayer(context: Context): Player = KohiiExoPlayer(
      context,
      clock,
      renderersFactory,
      trackSelectorFactory.createDefaultTrackSelector(context),
      loadControl,
      bandwidthMeterFactory.createBandwidthMeter(context),
      Util.getLooper()
  )
}
