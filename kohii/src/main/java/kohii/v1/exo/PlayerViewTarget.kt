/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
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

import android.view.ViewGroup
import androidx.core.view.contains
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Target

internal class PlayerViewTarget<V : ViewGroup>(val container: V) : Target<V, PlayerView> {

  override fun requireContainer(): V = container

  override fun attachPlayer(
    player: PlayerView
  ) {
    if (container is PlayerView || container === player) return
    if (!container.contains(player)) container.addView(player)
  }

  override fun detachPlayer(
    player: PlayerView
  ): Boolean {
    if (container is PlayerView || container === player) return false
    if (!container.contains(player)) return false
    container.removeView(player)
    return true
  }
}
