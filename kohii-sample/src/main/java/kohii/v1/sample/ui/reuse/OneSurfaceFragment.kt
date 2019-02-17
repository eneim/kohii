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

package kohii.v1.sample.ui.reuse

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.ContainerProvider
import kohii.v1.Kohii
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.reuse.data.Video
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView
import okio.buffer
import okio.source

@Suppress("unused")
@Keep
class OneSurfaceFragment : BaseFragment(), ContainerProvider {

  companion object {
    fun newInstance() = OneSurfaceFragment()
  }

  private val videos by lazy {
    val asset = requireActivity().application.assets
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val jsonAdapter: JsonAdapter<List<Video>> =
      moshi.adapter(Types.newParameterizedType(List::class.java, Video::class.java))
    return@lazy jsonAdapter.fromJson(asset.open("caminandes.json").source().buffer()) ?: emptyList()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, container, false)
  }

  val kohii: Kohii by lazy { Kohii[requireContext()] }
  private var scrollChangeListener: KohiiDemoScrollChangeListener? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val videoAdapter = VideoItemsAdapter(videos, kohii, this)

    recyclerView.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = videoAdapter
    }

    scrollChangeListener = KohiiDemoScrollChangeListener(videoAdapter).also {
      recyclerView.addOnScrollListener(it)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    scrollChangeListener?.let { recyclerView.removeOnScrollListener(it) }
    recyclerView.adapter = null
  }

  override fun provideContainers(): Array<Any>? {
    return arrayOf(recyclerView)
  }

  override fun provideContext(): Context {
    return requireContext()
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return viewLifecycleOwner
  }
}