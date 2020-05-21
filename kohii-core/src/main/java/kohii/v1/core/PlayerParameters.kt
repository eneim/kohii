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

package kohii.v1.core

data class PlayerParameters(
    // Video
  val maxVideoWidth: Int = Int.MAX_VALUE,
  val maxVideoHeight: Int = Int.MAX_VALUE,
  val maxVideoBitrate: Int = Int.MAX_VALUE,
    // Audio
  val maxAudioBitrate: Int = Int.MAX_VALUE
) {

  fun playerShouldStart() = maxAudioBitrate > 0
      || (maxVideoBitrate > 0 && maxVideoWidth > 0 && maxVideoHeight > 0)

  companion object {
    val DEFAULT = PlayerParameters()
  }
}
