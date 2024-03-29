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

package kohii.v1.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author eneim (2018/06/24).
 */
@Parcelize
data class PlaybackInfo(
  var resumeWindow: Int, // TODO rename to windowIndex
  var resumePosition: Long // TODO rename to position
) : Parcelable {

  constructor() : this(
    INDEX_UNSET,
    TIME_UNSET
  )

  companion object {
    const val TIME_UNSET = Long.MIN_VALUE + 1
    const val INDEX_UNSET = -1
  }
}
