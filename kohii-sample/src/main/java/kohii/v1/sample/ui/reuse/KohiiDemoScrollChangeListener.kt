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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.OnScrollListener

@Suppress("MemberVisibilityCanBePrivate")
internal class KohiiDemoScrollChangeListener(val playerManager: PlayerManager) : OnScrollListener() {

  override fun onScrollStateChanged(
    recyclerView: RecyclerView,
    newState: Int
  ) {
    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
    val candidates = recyclerView.findAllVideoHolder()
    candidates.playFirstPauseElse(
        doPlay = { holder -> playerManager.play(holder) },
        doPause = { holder -> playerManager.pause(holder) }
    ) {
      it.wantsToPlay()
    }
  }

  override fun onScrolled(
    recyclerView: RecyclerView,
    dx: Int,
    dy: Int
  ) {
    super.onScrolled(recyclerView, dx, dy)
    // Trigger an idle scroll. Eg: Kick off on first appearance.
    if (dx == 0) onScrollStateChanged(recyclerView, recyclerView.scrollState)
  }
}

// Shortcut to gather all VideoItemHolder.
internal fun RecyclerView.findAllVideoHolder(): Iterable<VideoItemHolder> {
  val candidates = ArrayList<VideoItemHolder>()
  layoutManager?.forEachIndexed { _, view ->
    val viewHolder = getChildViewHolder(view)
    if (viewHolder is VideoItemHolder) candidates.add(viewHolder)
  }
  return candidates
}

// Copy from androidx.core for ViewGroup.
inline fun LayoutManager.forEachIndexed(action: (index: Int, view: View) -> Unit) {
  for (index in 0 until childCount) {
    getChildAt(index)?.also { action(index, it) }
  }
}

inline fun <T> Iterable<T>.playFirstPauseElse(
  doPlay: (T) -> Unit,
  doPause: (T) -> Unit,
  predicate: (T) -> Boolean
) {
  var found = false
  var toPlay: T? = null
  for (element in this) {
    if (predicate(element)) {
      if (found) doPause(element)
      else {
        found = true
        toPlay = element
      }
    } else {
      doPause(element)
    }
  }

  if (found) doPlay(toPlay!!)
}
