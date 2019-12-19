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

package kohii.v1.sample.ui.grid

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import kohii.v1.core.MemoryMode
import kohii.v1.core.Rebinder
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.FragmentRecyclerviewGridBinding
import kotlin.LazyThreadSafetyMode.NONE

class GridContentFragment : BaseFragment() {

  override fun onAttach(context: Context) {
    super.onAttach(context)
    callback = parentFragment as? Callback
  }

  override fun onDetach() {
    super.onDetach()
    callback = null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentRecyclerviewGridBinding.inflate(inflater, container, false)
    return binding.root
  }

  private val kohii by lazy(NONE) { Kohii[this] }

  private lateinit var binding: FragmentRecyclerviewGridBinding
  private lateinit var adapter: ItemsAdapter
  private lateinit var selectionTracker: SelectionTracker<Rebinder>
  private lateinit var videoKeyProvider: VideoTagKeyProvider
  private lateinit var videoItemDetailsLookup: VideoItemDetailsLookup

  private var callback: Callback? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii.register(this, MemoryMode.BALANCED)
        .addBucket(binding.container)

    val spanCount = resources.getInteger(R.integer.grid_span)
    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int {
        return if (position % 6 == 3 || position % 6 == spanCount) 2 else 1
      }
    }

    adapter = ItemsAdapter(
        kohii,
        shouldBindVideo = { !selectionTracker.isSelected(it) },
        onVideoClick = { callback?.onSelected(it) }
    )

    (binding.container.layoutManager as? GridLayoutManager)?.spanSizeLookup = spanSizeLookup
    binding.container.adapter = adapter

    videoKeyProvider = VideoTagKeyProvider(binding.container)
    videoItemDetailsLookup = VideoItemDetailsLookup(binding.container)

    selectionTracker = SelectionTracker.Builder<Rebinder>(
        "${BuildConfig.APPLICATION_ID}::sample::grid",
        binding.container,
        videoKeyProvider,
        videoItemDetailsLookup,
        StorageStrategy.createParcelableStorage(Rebinder::class.java)
    )
        .withSelectionPredicate(SelectionPredicates.createSelectSingleAnything())
        .build()

    selectionTracker.onRestoreInstanceState(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    selectionTracker.onSaveInstanceState(outState)
  }

  internal fun select(rebinder: Rebinder) {
    selectionTracker.select(rebinder)
  }

  internal fun deselect(rebinder: Rebinder) {
    selectionTracker.deselect(rebinder)
  }

  interface Callback {

    fun onSelected(rebinder: Rebinder)
  }
}
