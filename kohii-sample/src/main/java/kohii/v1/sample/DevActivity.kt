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

package kohii.v1.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kohii.v1.exoplayer.DefaultControlDispatcher
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.common.BackPressConsumer
import kotlinx.android.synthetic.main.activity_debug.fragmentContainer
import kotlinx.android.synthetic.main.activity_debug.playerView1
import kotlinx.android.synthetic.main.activity_debug.playerView2

class DevActivity : AppCompatActivity() {

  companion object {
    const val videoUrl = "https://content.jwplatform.com/videos/Cl6EVHgQ-oQOe5Prq.mp4"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_debug)
    val kohii = Kohii[this]
    val manager = kohii.register(this)
        .addBucket(fragmentContainer)

    kohii.setUp(DemoApp.assetVideoUri) {
      tag = "player::1"
      // NestedScrollView
      // true, true --> (1) OK, (2) OK
      // false, true --> (1) OK, (2) OK
      // true, false --> (1) Failed, (2) OK
      // false, false --> (1) OK
      controller = DefaultControlDispatcher(
          manager, playerView1,
          kohiiCanStart = false,
          kohiiCanPause = false
      )
    }
        .bind(playerView1)

    kohii.setUp(DemoApp.assetVideoUri) {
      tag = "player::2"
    }
        .bind(playerView2)
  }

  override fun onBackPressed() {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
    if (currentFragment !is BackPressConsumer || !currentFragment.consumeBackPress()) {
      super.onBackPressed()
    }
  }
}
