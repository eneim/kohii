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

package kohii.v1.ads.exoplayer

import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory

/**
 * Configurations for a [PlayerViewImaBridge].
 *
 * @param adsLoader The [ImaAdsLoader] to construct a [PlayerViewImaBridge].
 * @param adsMediaSourceFactory The [MediaSourceFactory] to create the [MediaSource] for an Ad.
 */
class ImaBridgeConfig(
  val adsLoader: ImaAdsLoader,
  val adsMediaSourceFactory: MediaSourceFactory
)
