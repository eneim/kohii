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

@file:Suppress("UnusedImport")

package kohii.v1.sample

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseActivity
import kohii.v1.sample.ui.debug.DebugFragment
import kohii.v1.sample.ui.main.MainFragment
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
              DebugFragment.newInstance(), MainFragment::class.java.simpleName
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

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration?
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    val decorView = window.decorView
    if (isInPictureInPictureMode) {
      decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
          View.SYSTEM_UI_FLAG_FULLSCREEN
    } else {
      decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
  }
}

data class PlayerInfo(
  val adapterPos: Int,
  val viewTop: Int
)

// Implemented by host (Activity) to manage shared elements transition information.
interface PlayerInfoHolder {

  fun recordPlayerInfo(info: PlayerInfo?)

  fun fetchPlayerInfo(): PlayerInfo?
}
