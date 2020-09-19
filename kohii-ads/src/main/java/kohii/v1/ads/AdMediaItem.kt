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

package kohii.v1.ads

import android.net.Uri
import kohii.v1.media.MediaDrm
import kohii.v1.media.MediaItem
import kotlinx.android.parcel.Parcelize

/**
 * A default implementation of [AdMedia].
 */
@Parcelize
data class AdMediaItem(
  override val uri: Uri,
  override val adTagUri: Uri? = null,
  override val type: String? = null,
  override val mediaDrm: MediaDrm? = null
) : MediaItem(uri, type, mediaDrm), AdMedia
