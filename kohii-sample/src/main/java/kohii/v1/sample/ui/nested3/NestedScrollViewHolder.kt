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

package kohii.v1.sample.ui.nested3

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.parseAsHtml
import androidx.core.widget.NestedScrollView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Manager
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.R
import kohii.v1.sample.R.id
import kohii.v1.sample.R.string
import kohii.v1.sample.common.BaseViewHolder

internal class NestedScrollViewHolder(
  val manager: Manager,
  parent: ViewGroup
) : BaseViewHolder(parent, R.layout.holder_debug_nestsv) {

  private val container =
    itemView.findViewById(
        id.scrollViewContainer
    ) as AspectRatioFrameLayout
  private val scrollView = itemView.findViewById(
      id.scrollView
  ) as NestedScrollView
  private val playerView = itemView.findViewById(
      id.playerView
  ) as PlayerView
  private val libIntro = itemView.findViewById(
      id.libIntro
  ) as TextView

  init {
    container.setAspectRatio(4 / 5F)
    (playerView.findViewById(
        com.google.android.exoplayer2.ui.R.id.exo_content_frame
    ) as AspectRatioFrameLayout).setAspectRatio(16 / 9F)
  }

  override fun bind(item: Any?) {
    super.bind(item)
    libIntro.text = itemView.context.getString(string.lib_intro)
        .parseAsHtml()
  }

  override fun onAttached() {
    super.onAttached()
    manager.attach(scrollView)
    manager.master.setUp(assetVideoUri) {
      tag = "NESTED::NSV::$adapterPosition"
    }
        .bind(playerView)
  }

  override fun onDetached() {
    super.onDetached()
    manager.detach(scrollView)
  }
}
