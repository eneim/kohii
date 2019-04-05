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

package kohii.v1.sample

import android.os.Bundle
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseActivity
import kohii.v1.sample.ui.MainFragment
import kohii.v1.sample.ui.overlay.OverlayViewFragment
import kohii.v1.sample.ui.rview.RecyclerViewFragment.PlayerInfo
import kohii.v1.sample.ui.rview.RecyclerViewFragment.PlayerInfoHolder
import kotlinx.android.synthetic.main.main_activity.toolbar

class MainActivity : BaseActivity(), PlayerInfoHolder {

  private var playerInfo: PlayerInfo? = null

  override fun recordPlayerInfo(info: PlayerInfo?) {
    this.playerInfo = info
  }

  override fun fetchPlayerInfo() = playerInfo

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    setSupportActionBar(this.toolbar)
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .replace(
              R.id.fragmentContainer,
              OverlayViewFragment.newInstance(), MainFragment::class.java.simpleName
          )
          .commit()
    }
  }

  override fun onBackPressed() {
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
    if (currentFragment !is BackPressConsumer || !currentFragment.consumeBackPress()) {
      super.onBackPressed()
    }
  }
}
