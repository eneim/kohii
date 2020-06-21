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

package kohii.v1.sample.tiktok

import android.app.Application
import androidx.fragment.app.Fragment
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kohii.v1.sample.data.Video
import okio.buffer
import okio.source
import kotlin.LazyThreadSafetyMode.NONE

class TikTokApp : Application() {

  private val moshi: Moshi = Moshi.Builder()
      .build()

  val videos by lazy(NONE) {
    val jsonAdapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    jsonAdapter.fromJson(assets.open("caminandes.json").source().buffer()) ?: emptyList()
  }
}

fun Fragment.getApp(): TikTokApp = requireActivity().application as TikTokApp
