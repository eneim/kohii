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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import kohii.dev.Master
import kohii.media.MediaItem
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_debug.bindView1
import kotlinx.android.synthetic.main.fragment_debug.bindView2
import kotlinx.android.synthetic.main.fragment_debug.content
import kotlinx.android.synthetic.main.fragment_debug.playerView1
import kotlinx.android.synthetic.main.fragment_debug.playerView2
import kotlinx.android.synthetic.main.fragment_debug.scrollView

@Suppress("unused")
class DebugFragment : BaseFragment() {

  companion object {
    fun newInstance() = DebugFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val media = MediaItem(videoUrl)
    val content = content as LinearLayoutCompat

    val master = Master[this]
    master.register(this, scrollView)

    bindView1.setOnClickListener {
      master.setUp(media)
          .bind(playerView1) {
            Log.d("Kohii::Dev", "bound: $it")
          }
    }

    bindView2.setOnClickListener {
      master.setUp(media)
          .bind(playerView2) {
            Log.w("Kohii::Dev", "bound: $it")
          }
    }
  }
}
