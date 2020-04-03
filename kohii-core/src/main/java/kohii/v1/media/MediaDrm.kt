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
import androidx.core.util.ObjectsCompat
import java.util.Arrays

/**
 * Note: implementation of this interface must comparable using all 4 values, no more, no less.
 *
 * @author eneim (2018/06/25).
 */
interface MediaDrm : Comparable<MediaDrm>, Parcelable {

  // DRM Scheme
  val type: String

  val licenseUrl: String

  val keyRequestPropertiesArray: Array<String>?

  val multiSession: Boolean

  override fun compareTo(other: MediaDrm): Int {
    var result = type.compareTo(other.type)
    if (result == 0) {
      result = this.multiSession.compareTo(other.multiSession)
    }

    if (result == 0) {
      result = (if (ObjectsCompat.equals(this.licenseUrl, other.licenseUrl)) 0 else -1)
    }

    if (result == 0) {
      result = if (Arrays.deepEquals(keyRequestPropertiesArray, other.keyRequestPropertiesArray))
        0
      else
        -1
    }

    return result
  }
}
