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
package kohii.v1.sample.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Suppress("SpellCheckingInspection")
@JsonClass(generateAdapter = true)
@Parcelize
data class Playlist(
  val mediaid: String,
  val description: String,
  val pubdate: Int,
  val tags: String,
  val image: String,
  val title: String,
  val variations: Variations,
  val sources: List<Sources>,
  val tracks: List<Tracks>,
  val link: String,
  val duration: Int
) : Parcelable
