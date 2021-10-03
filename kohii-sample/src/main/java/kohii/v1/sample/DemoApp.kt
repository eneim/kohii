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
import android.net.Uri
import androidx.core.net.toUri
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import kohii.v1.ads.Manilo
import kohii.v1.sample.data.Item
import kohii.v1.sample.data.Video
import kohii.v1.sample.ui.ads.AdsContainerFragment
import kohii.v1.sample.ui.combo.ExoPlayerVideosFragment
import kohii.v1.sample.ui.echo.EchoFragment
import kohii.v1.sample.ui.fbook.FbookFragment
import kohii.v1.sample.ui.grid.GridRecyclerViewWithUserClickFragment
import kohii.v1.sample.ui.list.VerticalListRecyclerViewFragment
import kohii.v1.sample.ui.main.DemoItem
import kohii.v1.sample.ui.manual.ManualRecyclerViewFragment
import kohii.v1.sample.ui.motion.MotionFragment
import kohii.v1.sample.ui.mstdtl.MasterDetailFragment
import kohii.v1.sample.ui.nested1.VerticalRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.nested2.HorizontalRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.nested3.NestedScrollViewInsideRecyclerViewFragment
import kohii.v1.sample.ui.nested4.VerticalFixedHeightRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.nested5.RecyclerViewInsideRecyclerViewFragment
import kohii.v1.sample.ui.overlay.OverlayViewFragment
import kohii.v1.sample.ui.pagers.ViewPager1WithFragmentsFragment
import kohii.v1.sample.ui.pagers.ViewPager1WithRecyclerViewFragmentsFragment
import kohii.v1.sample.ui.pagers.ViewPager1WithViewsFragment
import kohii.v1.sample.ui.pagers.ViewPager2WithFragmentsFragment
import kohii.v1.sample.ui.pagers.ViewPager2WithRecyclerViewFragmentsFragment
import kohii.v1.sample.ui.pagers.ViewPager2WithViewsFragment
import kohii.v1.sample.ui.sview.ScrollViewFragment
import kohii.v1.sample.ui.youtube1.YouTube1Fragment
import kohii.v1.sample.ui.youtube2.YouTube2Fragment
import okio.buffer
import okio.source
import timber.log.Timber
import timber.log.Timber.DebugTree
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author eneim (2018/06/26).
 */
@Suppress("unused")
class DemoApp : Application() {

  companion object {

    const val assetVideoUri = "file:///android_asset/bbb_45s_hevc.webm"
  }

  internal val moshi: Moshi = Moshi.Builder()
      .add(object : JsonAdapter<Uri>() {
        @FromJson
        override fun fromJson(reader: JsonReader): Uri = reader.nextString().toUri()

        @ToJson
        override fun toJson(writer: JsonWriter, value: Uri?) {
          writer.value(value.toString())
        }

        override fun toString(): String = "JsonAdapter(Uri)"
      })
      .build()

  val videos by lazy(NONE) {
    val jsonAdapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    jsonAdapter.fromJson(assets.open("caminandes.json").source().buffer()) ?: emptyList()
  }

  val exoItems: List<Item> by lazy(NONE) {
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    adapter.fromJson(assets.open("medias.json").source().buffer()) ?: emptyList()
  }

  val youtubeApiKey by lazy(NONE) {
    val keyId = resources.getIdentifier("google_api_key", "string", packageName)
    return@lazy if (keyId > 0) getString(keyId) else ""
  }

  val demoItems by lazy(NONE) {
    @Suppress("UNUSED_VARIABLE")
    val youtubeDemos: Collection<DemoItem> = if (youtubeApiKey.isNotEmpty()) {
      listOf(
          DemoItem(
              R.string.demo_title_youtube_1,
              R.string.demo_desc_youtube_1,
              YouTube1Fragment::class.java
          ),
          DemoItem(
              R.string.demo_title_youtube_2,
              R.string.demo_desc_youtube_2,
              YouTube2Fragment::class.java
          )
      )
    } else {
      emptyList()
    }

    (setOf(
        DemoItem(
            R.string.demo_title_recycler_view_0,
            R.string.demo_desc_recycler_view_0,
            GridRecyclerViewWithUserClickFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_manual_1,
            R.string.demo_desc_manual_1,
            ManualRecyclerViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_ads_1,
            R.string.demo_desc_ads_1,
            AdsContainerFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_fbook,
            R.string.demo_desc_fbook,
            FbookFragment::class.java
        )
    ) + youtubeDemos + setOf(
        DemoItem(
            R.string.demo_title_recycler_view_1,
            R.string.demo_desc_recycler_view_1,
            VerticalListRecyclerViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_2,
            R.string.demo_desc_recycler_view_2,
            ExoPlayerVideosFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_3,
            R.string.demo_desc_recycler_view_3,
            OverlayViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_recycler_view_4,
            R.string.demo_desc_recycler_view_4,
            EchoFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_nested_scrollview_1,
            R.string.demo_desc_nested_scrollview_1,
            MotionFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_nested_scrollview_2,
            R.string.demo_desc_nested_scrollview_2,
            ScrollViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_1,
            R.string.demo_desc_pager_1,
            ViewPager1WithFragmentsFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_2,
            R.string.demo_desc_pager_2,
            ViewPager1WithViewsFragment::class.java
        ),
        DemoItem(
            0,
            0,
            ViewPager1WithRecyclerViewFragmentsFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_3,
            R.string.demo_desc_pager_3,
            ViewPager2WithFragmentsFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_4,
            R.string.demo_desc_pager_4,
            ViewPager2WithViewsFragment::class.java
        ),
        DemoItem(
            0,
            0,
            ViewPager2WithRecyclerViewFragmentsFragment::class.java
        ),
        DemoItem(
            0, 0, NestedScrollViewInsideRecyclerViewFragment::class.java
        ),
        DemoItem(
            0, 0, VerticalRecyclerViewInsideNestedScrollViewFragment::class.java
        ),
        DemoItem(
            0, 0, HorizontalRecyclerViewInsideNestedScrollViewFragment::class.java
        ),
        DemoItem(
            0, 0, VerticalFixedHeightRecyclerViewInsideNestedScrollViewFragment::class.java
        ),
        DemoItem(
            0, 0, RecyclerViewInsideRecyclerViewFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_master_detail,
            R.string.demo_desc_master_detail,
            MasterDetailFragment::class.java
        )
    )).toList()
  }

  val manilo: Manilo by lazy(NONE) {
    Manilo[this]
  }

  @Suppress("RedundantOverride")
  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(DebugTree())
    }
  }
}
