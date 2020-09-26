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
import androidx.recyclerview.selection.SelectionTracker.Builder
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import kohii.v1.core.MemoryMode
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.FragmentRecyclerviewGridBinding

class GridContentFragment : BaseFragment() {

  private lateinit var kohii: Kohii

  private var _binding: FragmentRecyclerviewGridBinding? = null
  private val binding: FragmentRecyclerviewGridBinding get() = requireNotNull(_binding)

  private var _selectionTracker: SelectionTracker<SelectionKey>? = null
  private val selectionTracker: SelectionTracker<SelectionKey>
    get() = requireNotNull(_selectionTracker)

  private var videoGridCallback: VideoGridCallback? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    kohii = Kohii[this]
    videoGridCallback = parentFragment as? VideoGridCallback
  }

  override fun onDetach() {
    super.onDetach()
    videoGridCallback = null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentRecyclerviewGridBinding.inflate(inflater, container, false)
    return binding.root
  }

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

    val adapter = ItemsAdapter(
        kohii,
        shouldBindVideo = { !selectionTracker.isSelected(it) },
        onVideoClick = { videoGridCallback?.onSelected(it) }
    )

    (binding.container.layoutManager as? GridLayoutManager)?.spanSizeLookup = spanSizeLookup
    binding.container.adapter = adapter

    val videoKeyProvider = VideoTagKeyProvider(binding.container)
    val videoItemDetailsLookup = VideoItemDetailsLookup(binding.container)

    _selectionTracker = Builder(
        "${BuildConfig.APPLICATION_ID}::sample::grid",
        binding.container,
        videoKeyProvider,
        videoItemDetailsLookup,
        StorageStrategy.createParcelableStorage(SelectionKey::class.java)
    )
        .withSelectionPredicate(SelectionPredicates.createSelectSingleAnything())
        .build()

    selectionTracker.onRestoreInstanceState(savedInstanceState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    selectionTracker.onSaveInstanceState(outState)
  }

  internal fun select(selectionKey: SelectionKey) {
    selectionTracker.select(selectionKey)
  }

  internal fun deselect(selectionKey: SelectionKey) {
    selectionTracker.deselect(selectionKey)
    binding.container.adapter?.notifyItemChanged(selectionKey.position)
  }

  interface VideoGridCallback {

    fun onSelected(selectionKey: SelectionKey)
  }
}
