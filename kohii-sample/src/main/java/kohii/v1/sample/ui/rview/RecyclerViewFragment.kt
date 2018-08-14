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

package kohii.v1.sample.ui.rview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.doOnNextLayoutAs
import kohii.v1.sample.common.toPixel
import okio.Okio


/**
 * @author eneim (2018/07/06).
 */
class RecyclerViewFragment : BaseFragment() {

  companion object {
    fun newInstance() = RecyclerViewFragment()
  }

  data class PlayerInfo(val adapterPos: Int, val viewTop: Int)

  // implemented by host (Activity) to manage shared elements transition information.
  interface PlayerInfoHolder {

    fun recordPlayerInfo(info: PlayerInfo?)

    fun fetchPlayerInfo(): PlayerInfo?
  }

  private var items: List<Item>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val asset = requireActivity().assets
    val type = Types.newParameterizedType(List::class.java, Item::class.java)
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter: JsonAdapter<List<Item>> = moshi.adapter(type)
    items = adapter.fromJson(Okio.buffer(Okio.source(asset.open("theme.json"))))
  }

  override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_recycler_view, parent, false)
  }

  private var container: RecyclerView? = null

  // Should be implemented by Activity, to keep information of latest clicked item position.
  private var playerInfoHolder: PlayerInfoHolder? = null

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    playerInfoHolder = context as? PlayerInfoHolder?
  }

  override fun onDetach() {
    super.onDetach()
    playerInfoHolder = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (items == null) return

    prepareTransitions()
    postponeEnterTransition()

    val data = ArrayList(items!!).apply { this.addAll(items!!) }
    container = (view.findViewById(R.id.recyclerView) as RecyclerView).also {
      it.setHasFixedSize(true)
      it.layoutManager = LinearLayoutManager(
          requireContext()).apply { this.isItemPrefetchEnabled = true }
      it.adapter = ItemsAdapter(this, data) { dp -> dp.toPixel(resources) }
    }

    this.playerInfoHolder?.fetchPlayerInfo()?.run {
      container!!.doOnNextLayoutAs<RecyclerView> {
        val layout = it.layoutManager as LinearLayoutManager
        val viewAtPosition = layout.findViewByPosition(this.adapterPos)
        // Scroll to position if the view for the current position is null (not currently part of
        // layout manager children), or it's not completely visible.
        if (viewAtPosition == null || layout.isViewPartiallyVisible(viewAtPosition, false, true)) {
          it.postDelayed(200) {
            layout.scrollToPositionWithOffset(this.adapterPos, this.viewTop)
          }
        }
      }
    }
  }

  private fun prepareTransitions() {
    // Hmm Google https://stackoverflow.com/questions/49461738/transitionset-arraylist-size-on-a-null-object-reference
    val transition = TransitionInflater.from(requireContext())
        .inflateTransition(R.transition.player_exit_transition)
    transition.duration = 375
    exitTransition = transition

    val playerInfo = this.fetchPlayerInfo() ?: return
    setEnterSharedElementCallback(object : SharedElementCallback() {
      override fun onMapSharedElements(names: List<String>?, elements: MutableMap<String, View>?) {
        // Locate the ViewHolder for the clicked position.
        val holder = container?.findViewHolderForAdapterPosition(playerInfo.adapterPos)
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