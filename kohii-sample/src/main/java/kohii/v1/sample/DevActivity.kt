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
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.DemoApp.Companion.assetVideoUri
import kohii.v1.sample.common.BackPressConsumer
import kotlinx.android.synthetic.main.activity_debug.fragmentContainer
import kotlinx.android.synthetic.main.activity_debug.playerView1
import kotlinx.android.synthetic.main.activity_debug.playerView2

class DevActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_debug)
    val kohii = Kohii[this]
    kohii.register(this)
        .attach(fragmentContainer)

    val tag = assetVideoUri
    kohii.setUp(assetVideoUri) {
      this.tag = tag
    }
        .bind(playerView1)
    kohii.setUp(assetVideoUri) {
      this.tag = tag
    }
        .bind(playerView2)
    kohii.cancel(playerView2)
  }

  override fun onBackPressed() {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
    if (currentFragment !is BackPressConsumer || !currentFragment.consumeBackPress()) {
      super.onBackPressed()
    }
  }
}
