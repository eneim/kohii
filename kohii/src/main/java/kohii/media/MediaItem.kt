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

package kohii.media

import android.net.Uri
import kotlinx.android.parcel.Parcelize

/**
 * @author eneim (2018/10/19).
 */
@Parcelize
open class MediaItem(
  override val uri: Uri,
  override val type: String? = null,
  override val mediaDrm: MediaDrm? = null
) : Media {

  constructor(
    url: String,
    type: String? = null,
    mediaDrm: MediaDrm? = null
  ) : this(Uri.parse(url), type, mediaDrm)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MediaItem

    if (uri != other.uri) return false
    if (type != other.type) return false
    if (mediaDrm != other.mediaDrm) return false

    return true
  }

  override fun hashCode(): Int {
    var result = uri.hashCode()
    result = 31 * result + (type?.hashCode() ?: 0)
    result = 31 * result + (mediaDrm?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "K::Media(uri=$uri, type=$type, mediaDrm=$mediaDrm)"
  }

}