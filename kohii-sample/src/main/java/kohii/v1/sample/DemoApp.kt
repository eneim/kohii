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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.sample.data.Item
import kohii.v1.sample.data.Video
import kohii.v1.sample.ui.combo.ExoPlayerVideosFragment
import kohii.v1.sample.ui.debug.GridContentFragment
import kohii.v1.sample.ui.main.DemoItem
import kohii.v1.sample.ui.mix.MixMediaFragment
import kohii.v1.sample.ui.motion.MotionFragment
import kohii.v1.sample.ui.nested1.VerticalRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.nested2.HorizontalRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.nested3.NestedScrollViewInsideRecyclerViewFragment
import kohii.v1.sample.ui.nested4.VerticalFixHeightRecyclerViewInsideNestedScrollViewFragment
import kohii.v1.sample.ui.sview.ScrollViewFragment
import kohii.v1.sample.ui.youtube1.YouTube1Fragment
import kohii.v1.sample.ui.youtube2.YouTube2Fragment
import okio.buffer
import okio.source
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author eneim (2018/06/26).
 */
@Suppress("unused")
class DemoApp : Application() {

  companion object {
    const val assetVideoUri = "file:///android_asset/bbb_45s_hevc.mp4"
  }

  private val moshi: Moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()

  // shared between demos
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

    listOf(
        DemoItem(fragmentClass = ExoPlayerVideosFragment::class.java),
        DemoItem(fragmentClass = GridContentFragment::class.java),
        DemoItem(fragmentClass = MixMediaFragment::class.java),
        DemoItem(fragmentClass = MotionFragment::class.java),
        DemoItem(fragmentClass = ScrollViewFragment::class.java),
        DemoItem(fragmentClass = VerticalRecyclerViewInsideNestedScrollViewFragment::class.java),
        DemoItem(
            fragmentClass = VerticalFixHeightRecyclerViewInsideNestedScrollViewFragment::class.java
        ),
        DemoItem(fragmentClass = HorizontalRecyclerViewInsideNestedScrollViewFragment::class.java),
        DemoItem(fragmentClass = NestedScrollViewInsideRecyclerViewFragment::class.java)
        /* , DemoItem(
            R.string.demo_title_debug,
            R.string.demo_desc_debug,
            GridContentFragment::class.java
        ), DemoItem(
            R.string.demo_title_fbook,
            R.string.demo_desc_fbook,
            FbookFragment::class.java
        ) */
    ) /* + youtubeDemos + listOf(
        DemoItem(
            R.string.demo_title_recycler_view_1,
            R.string.demo_desc_recycler_view_1,
            RecyclerViewFragment::class.java
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
        ), DemoItem(
            R.string.demo_title_pager_1,
            R.string.demo_desc_pager_1,
            Pager1Fragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_2,
            R.string.demo_desc_pager_2,
            PagerViewsFragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_3,
            R.string.demo_desc_pager_3,
            Pager2Fragment::class.java
        ),
        DemoItem(
            R.string.demo_title_pager_4,
            R.string.demo_desc_pager_4,
            Pager3Fragment::class.java
        ),
        DemoItem(
            R.string.demo_title_master_detail,
            R.string.demo_desc_master_detail,
            MasterDetailFragment::class.java
        )
    ) */
  }

  @Suppress("RedundantOverride")
  override fun onCreate() {
    super.onCreate()
  }
}
