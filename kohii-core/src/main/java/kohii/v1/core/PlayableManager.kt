/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v1.core

import kohii.v1.media.PlaybackInfo

/**
 * Common definitions for a class that manages [Playable]s.
 *
 * @see [Master]
 * @see [Manager]
 */
interface PlayableManager {

  /**
   * Adds a new [Playable] to this manager.
   */
  fun addPlayable(playable: Playable)

  /**
   * Removes a [Playable] from this manager.
   */
  fun removePlayable(playable: Playable)

  /**
   * Saves the [PlaybackInfo] of the [playable].
   */
  fun trySavePlaybackInfo(playable: Playable)

  /**
   * Restores the [PlaybackInfo] for the [playable] if it was saved before.
   */
  fun tryRestorePlaybackInfo(playable: Playable)
}
