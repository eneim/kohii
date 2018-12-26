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

package kohii.v1.sample.ui.mix

import android.os.Parcelable
import com.squareup.moshi.Json
import kohii.media.MediaDrm
import kotlinx.android.parcel.Parcelize

/**
 * @author eneim (2018/10/30).
 */
@Parcelize
data class Item(
    val name: String,
    val uri: String,
    val extension: String?,
    @Json(name = "drm_scheme") val drmScheme: String?,
    @Json(name = "drm_license_url") val drmLicenseUrl: String?
) : Parcelable

@Parcelize
data class DrmItem(
    val item: Item
) : MediaDrm {
  override val type: String
    get() = item.drmScheme!!
  override val licenseUrl: String?
    get() = item.drmLicenseUrl
  override val keyRequestPropertiesArray: Array<String>?
    get() = null
  override val multiSession: Boolean
    get() = false
}