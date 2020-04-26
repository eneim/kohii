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

package kohii.v1.dev

import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseViewHolder

internal class DummyViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.dev_video_holder) {
  internal val playerView: PlayerView = itemView.findViewById(R.id.playerView)
  internal val enterFullscreen: View = itemView.findViewById(R.id.exo_fullscreen_enter)
}
