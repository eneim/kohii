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

@Deprecated("From 1.1.0+, this data is no longer used anymore.")
data class VideoSize(
  val maxWidth: Int,
  val maxHeight: Int
) {
  companion object {
    val NONE = VideoSize(Int.MIN_VALUE, Int.MIN_VALUE)
    val SD = VideoSize(720 /* auto */, 480)
    val HD = VideoSize(1280 /* auto */, 720)
    val FHD = VideoSize(1920 /* auto */, 1080)
    val UHD = VideoSize(3840 /* auto */, 2160)
    val ORIGINAL = VideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
  }
}
