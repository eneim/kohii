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

package kohii.v1.exo

import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import kohii.media.MediaDrm

/**
 * @author eneim (2018/06/25).
 */
internal class DefaultPlayerFactory(
    private val store: ExoStore,
    private val config: Config
) : PlayerFactory {

  private val renderersFactory: RenderersFactory  // stateless
  private val trackSelector: TrackSelector  // 'maybe' stateless
  private val loadControl: LoadControl  // stateless
  private val drmSessionManagerFactory: DrmSessionManagerFactory

  init {
    trackSelector = DefaultTrackSelector()
    loadControl = DefaultLoadControl()
    renderersFactory = DefaultRenderersFactory(store.context, config.extensionMode)
    drmSessionManagerFactory = DefaultDrmSessionManagerFactory(store)
  }

  override fun createPlayer(mediaDrm: MediaDrm?): Player {
    return KohiiPlayer(
        store.context,
        renderersFactory,
        trackSelector,
        loadControl,
        config.meter,
        if (mediaDrm != null)
          drmSessionManagerFactory.createDrmSessionManager(mediaDrm)
        else
          null
    )
  }
}
