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

package kohii.v1.core

enum class MemoryMode {
  /**
   * In AUTO mode, Kohii will judge the preferred memory situation using [Master.preferredMemoryMode]
   * method.
   */
  AUTO,

  /**
   * In LOW mode, Kohii will always release resource of unselected Playables/Playbacks
   * (whose distance to selected ones are from 1).
   */
  LOW,

  /**
   * In NORMAL mode, Kohii will only reset the Playables/Playbacks whose distance to selected ones
   * are 1 (so 'next to' selected ones). Others will be released.
   */
  NORMAL,

  /**
   * In BALANCED mode, the release behavior is the same with 'NORMAL' mode, but unselected
   * Playables/Playbacks will not be reset.
   */
  BALANCED,

  /**
   * HIGH mode must be specified by client.
   *
   * In HIGH mode, any unselected Playables/Playbacks whose distance to selected ones is less
   * than 8 will be reset. Others will be released. This mode is memory-intensive and can be
   * used in many-videos-yet-low-memory-usage scenario like simple/short Videos.
   */
  HIGH,

  /**
   * "For the bravest only"
   *
   * INFINITE mode must be specified by client.
   *
   * In INFINITE mode, no unselected Playables/Playbacks will ever be released due to distance
   * change (though Kohii will release the resource once they are inactive).
   */
  INFINITE
}
