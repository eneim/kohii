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

package kohii.v1.sample.ui.debug

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.parseAsHtml
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.core.Master
import kohii.core.Master.MemoryMode
import kohii.core.PlayerViewRebinder
import kohii.core.Rebinder
import kohii.v1.sample.BuildConfig
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.BaseViewHolder
import kotlinx.android.synthetic.main.fragment_debug_child.container
import kotlin.LazyThreadSafetyMode.NONE

internal class TextViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.widget_simple_textview) {

  val textContent = itemView as TextView

  init {
    textContent.maxLines = 5
  }

  override fun bind(item: Any?) {
    super.bind(item)
    textContent.text = itemView.context.getString(R.string.lib_intro)
        .parseAsHtml()
  }
}

internal class VideoViewHolder(
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_player_view) {
  internal val container = itemView.findViewById(R.id.playerContainer) as AspectRatioFrameLayout

  init {
    container.setAspectRatio(16 / 9F)
  }

  internal var videoUrl: String? = null
  internal val videoTag: String?
    get() = videoUrl?.let { "HOLDER::〜${adapterPosition}" }

  internal val rebinder: Rebinder<PlayerView>?
    get() = videoTag?.let { PlayerViewRebinder(it) }

  internal val itemDetails: ItemDetails<Rebinder<*>>
    get() = object : ItemDetails<Rebinder<*>>() {
      override fun getSelectionKey() = rebinder
      override fun getPosition() = adapterPosition
    }
}

internal class ItemsAdapter(
  private val master: Master,
  val shouldBindVideo: (Rebinder<*>?) -> Boolean,
  val onVideoClick: (Rebinder<*>) -> Unit
) : Adapter<BaseViewHolder>() {

  companion object {
    private const val TYPE_VIDEO = 1
    private const val TYPE_TEXT = 2
  }

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder {
    return if (viewType == TYPE_VIDEO)
      VideoViewHolder(parent).also { holder ->
        holder.itemView.setOnClickListener {
          holder.rebinder?.let(onVideoClick)
        }
      } else TextViewHolder(parent)
  }

  override fun getItemCount(): Int {
    return Int.MAX_VALUE
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemViewType(position: Int): Int {
    return if (position % 6 == 3) TYPE_VIDEO else TYPE_TEXT
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder,
    position: Int
  ) {
    if (holder is VideoViewHolder) {
      holder.videoUrl = assetVideoUri
      val videoTag = holder.videoTag
      Log.d("Kohii::Dev", "$videoTag updated")
      if (shouldBindVideo(holder.rebinder)) {
        master.setUp(assetVideoUri)
            .with { tag = videoTag }
            .bind(holder.container)
      }
    } else holder.bind(position)
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    if (holder is VideoViewHolder) holder.videoUrl = null
  }
}

class DebugChildFragment : BaseFragment() {

  private var callback: Callback? = null

  private val master by lazy(NONE) { Master[this] }
  private val adapter by lazy(NONE) {
    ItemsAdapter(
        master,
        shouldBindVideo = { !selectionTracker.isSelected(it) },
        onVideoClick = { callback?.onSelected(it) }
    )
  }

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
    return inflater.inflate(R.layout.fragment_debug_child, container, false)
  }

  private lateinit var selectionTracker: SelectionTracker<Rebinder<*>>
  private lateinit var videoKeyProvider: VideoTagKeyProvider
  private lateinit var videoItemDetailsLookup: VideoItemDetailsLookup

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    master.register(this, MemoryMode.BALANCED)
        .attach(container)

    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int {
        return if (position % 6 == 3) 2 else 1
      }
    }

    (container.layoutManager as? GridLayoutManager)?.spanSizeLookup = spanSizeLookup
    container.adapter = adapter

    videoKeyProvider = VideoTagKeyProvider(container)
    videoItemDetailsLookup = VideoItemDetailsLookup(container)

    selectionTracker = SelectionTracker.Builder<Rebinder<*>>(
        "${BuildConfig.APPLICATION_ID}::sample::debug",
        container,
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

  internal fun select(rebinder: Rebinder<*>) {
    selectionTracker.select(rebinder)
  }

  internal fun deselect(rebinder: Rebinder<*>) {
    selectionTracker.deselect(rebinder)
  }

  interface Callback {

    fun onSelected(rebinder: Rebinder<*>)
  }
}

// ItemKeyProvider

internal class VideoTagKeyProvider(
  val recyclerView: RecyclerView
) : ItemKeyProvider<Rebinder<*>>(SCOPE_CACHED) {

  private val posToKey = SparseArray<Rebinder<*>>()
  private val keyToPos = HashMap<Rebinder<*>, Int>()

  init {
    require(recyclerView.adapter?.hasStableIds() == true)
    recyclerView.addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
      override fun onChildViewDetachedFromWindow(view: View) {
        onDetached(view)
      }

      override fun onChildViewAttachedToWindow(view: View) {
        onAttached(view)
      }
    })
  }

  override fun getKey(position: Int): Rebinder<*>? {
    return posToKey[position]
  }

  override fun getPosition(key: Rebinder<*>): Int {
    return keyToPos[key] ?: NO_POSITION
  }

  internal fun onAttached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view) as? VideoViewHolder ?: return
    val id = holder.itemId
    if (id != NO_ID) {
      val position = holder.adapterPosition
      val key = holder.rebinder
      if (position != NO_POSITION && key != null) {
        posToKey.put(position, key)
        keyToPos[key] = position
      }
    }
  }

  internal fun onDetached(view: View) {
    val holder = recyclerView.findContainingViewHolder(view) as? VideoViewHolder ?: return
    val id = holder.itemId
    // only if id == NO_ID, we remove this View from cache.
    // when id != NO_ID, it means that this View is still bound to an Item.
    if (id == NO_ID) {
      val position = holder.adapterPosition
      val key = holder.rebinder
      if (position != NO_POSITION && key != null) {
        posToKey.remove(position)
        keyToPos.remove(key)
      }
    }
  }
}

internal class VideoItemDetailsLookup(val recyclerView: RecyclerView) : ItemDetailsLookup<Rebinder<*>>() {
  override fun getItemDetails(event: MotionEvent): ItemDetails<Rebinder<*>>? {
    val view = recyclerView.findChildViewUnder(event.x, event.y) ?: return null
    val holder = recyclerView.findContainingViewHolder(view) as? VideoViewHolder ?: return null
    return holder.itemDetails
  }
}
