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

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import kohii.v1.sample.common.BackPressConsumer
import kohii.v1.sample.common.BaseActivity
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.common.DemoContainer
import kohii.v1.sample.common.splitCases
import kohii.v1.sample.databinding.MainActivityBinding
import kohii.v1.sample.ui.combo.LandscapeFullscreenFragment
import kohii.v1.sample.ui.main.MainListFragment

class MainActivity : BaseActivity(), PlayerInfoHolder, LandscapeFullscreenFragment.Callback {

  private lateinit var binding: MainActivityBinding
  private var playerInfo: PlayerInfo? = null

  override fun recordPlayerInfo(info: PlayerInfo?) {
    this.playerInfo = info
  }

  override fun fetchPlayerInfo() = playerInfo

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = MainActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)

    supportFragmentManager.registerFragmentLifecycleCallbacks(
        object : FragmentLifecycleCallbacks() {
          override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            if (f !is BaseFragment) return
            if (f !is DemoContainer) {
              this@MainActivity.updateTitle(getString(R.string.app_name))
              return
            }
            val titleId = f.demoItem?.title ?: 0
            val title = if (titleId == 0) {
              f.javaClass.simpleName.splitCases()
            } else {
              getString(titleId)
            }
            this@MainActivity.updateTitle(title)
          }

          override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
            if (f is DemoContainer) this@MainActivity.updateTitle(getString(R.string.app_name))
          }
        }, false
    )

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .replace(
              R.id.fragmentContainer,
              MainListFragment.newInstance(),
              MainListFragment::class.java.simpleName
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

  internal fun updateTitle(title: String) {
    binding.toolbarLayout.title = title
  }

  // LandscapeFullscreenFragment.Callback

  override fun hideToolbar() {
    binding.appBarLayout.isVisible = false
  }

  override fun showToolbar() {
    binding.appBarLayout.isVisible = true
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
