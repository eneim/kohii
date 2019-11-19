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

package kohii.v1.sample.ui.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.app.SharedElementCallback
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.core.Master
import kohii.v1.sample.PlayerInfo
import kohii.v1.sample.PlayerInfoHolder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.doOnNextLayoutAs
import kohii.v1.sample.common.getApp
import kohii.v1.sample.common.toPixel
import kohii.v1.sample.ui.list.data.Item
import kotlinx.android.synthetic.main.fragment_recycler_view.recyclerView
import okio.buffer
import okio.source

/**
 * @author eneim (2018/07/06).
 */
@Keep
class VerticalListRecyclerViewFragment : BaseFragment() {

  companion object {
    fun newInstance() = VerticalListRecyclerViewFragment()
  }

  private val items: List<Item> by lazy {
    val asset = getApp().assets
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    return@lazy adapter.fromJson(asset.open("theme.json").source().buffer())!!
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    state: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, parent, false)
  }

  // Should be implemented by Activity, to keep information of latest clicked item position.
  private var playerInfoHolder: PlayerInfoHolder? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    playerInfoHolder = context as? PlayerInfoHolder?
  }

  override fun onDetach() {
    super.onDetach()
    playerInfoHolder = null
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val kohii = Master[this].also {
      it.register(this)
          .attach(recyclerView)
    }

    val data = ArrayList(items).apply { this.addAll(items) } // To double the list.
    val container = (view.findViewById(R.id.recyclerView) as RecyclerView).also {
      it.setHasFixedSize(true)
      it.layoutManager = LinearLayoutManager(requireContext())
      it.adapter = ItemsAdapter(kohii, this, data) { dp -> dp.toPixel(resources) }
    }

    prepareTransitions(container)
    postponeEnterTransition()

    this.playerInfoHolder?.fetchPlayerInfo()
        ?.run {
          container.doOnNextLayoutAs<RecyclerView> {
            val layout = it.layoutManager as LinearLayoutManager
            val viewAtPosition = layout.findViewByPosition(this.adapterPos)
            // Scroll to position if the view for the current position is null (not currently part of
            // layout manager children), or it's not completely visible.
            if (viewAtPosition == null ||
                layout.isViewPartiallyVisible(viewAtPosition, false, true)
            ) {
              it.postDelayed(200) {
                layout.scrollToPositionWithOffset(this.adapterPos, this.viewTop)
              }
            }
          }
        }
  }

  private fun prepareTransitions(container: RecyclerView) {
    // Hmm Google https://stackoverflow.com/questions/49461738/transitionset-arraylist-size-on-a-null-object-reference
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_exit_transition)
    transition.duration = 375
    exitTransition = transition

    val playerInfo = this.fetchPlayerInfo() ?: return
    setEnterSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(
        names: List<String>?,
        elements: MutableMap<String, View>?
      ) {
        // Locate the ViewHolder for the clicked position.
        val holder = container.findViewHolderForAdapterPosition(playerInfo.adapterPos)
        if (holder is VideoViewHolder) {
          // Map the first shared element name to the child ImageView.
          elements?.put(names?.get(0)!!, holder.transView)
        } else {
          return
        }
      }
    })
  }

  // Called by Adapter
  fun recordPlayerInfo(playerInfo: PlayerInfo?) {
    this.playerInfoHolder?.recordPlayerInfo(playerInfo)
  }

  // Called by Adapter
  fun fetchPlayerInfo() = this.playerInfoHolder?.fetchPlayerInfo()
}
