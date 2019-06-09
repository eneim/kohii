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

package kohii.v1.sample

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.fabric.sdk.android.Fabric
import kohii.v1.sample.data.Item
import kohii.v1.sample.data.Video
import okio.buffer
import okio.source
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author eneim (2018/06/26).
 */
@Suppress("unused")
class DemoApp : Application() {

  // shared between demos
  val videos by lazy(NONE) {
    val asset = assets
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val jsonAdapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    jsonAdapter.fromJson(asset.open("caminandes.json").source().buffer()) ?: emptyList()
  }

  val exoItems: List<Item> by lazy {
    val asset = assets
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    adapter.fromJson(asset.open("medias.json").source().buffer()) ?: emptyList()
  }

  override fun onCreate() {
    super.onCreate()
    Fabric.with(this, Crashlytics())
  }
}
