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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.Kohii
import kohii.v1.LifecycleOwnerProvider
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView
import okio.buffer
import okio.source

/**
 * @author eneim (2018/10/30).
 */
@Suppress("unused")
@Keep
class MixMediaFragment : BaseFragment(), LifecycleOwnerProvider {

  companion object {
    fun newInstance() = MixMediaFragment()
  }

  val kohii: Kohii by lazy {
    Kohii[this].also {
      it.register(this, arrayOf(recyclerView))
    }
  }

  private val videos: List<Item> by lazy {
    val asset = requireActivity().application.assets
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    adapter.fromJson(asset.open("medias.json").source().buffer()) ?: emptyList()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, parent, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Kohii[this].also { it.register(this, arrayOf(recyclerView)) }

    (recyclerView as RecyclerView).also {
      it.setHasFixedSize(true)
      it.adapter = ItemsAdapter(videos, kohii)
    }
  }

  override fun provideLifecycleOwner(): LifecycleOwner {
    return viewLifecycleOwner
  }
}
