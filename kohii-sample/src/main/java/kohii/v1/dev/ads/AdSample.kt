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

package kohii.v1.dev.ads

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AdSample(
  @Json(name = "name")
  val name: String,
  @Json(name = "uri")
  val contentUri: Uri,
  @Json(name = "ad_tag_uri")
  val adTagUri: Uri
) : Parcelable

@JsonClass(generateAdapter = true)
data class AdSamples(
  @Json(name = "name")
  val name: String,
  @Json(name = "samples")
  val samples: List<AdSample>
)
