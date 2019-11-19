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

package kohii.v1.sample.ui.motion

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import kohii.v1.core.Master
import kohii.v1.core.Rebinder
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseActivity
import kohii.v1.sample.common.InitData
import kotlinx.android.synthetic.main.activity_player.playerContainer
import kotlinx.android.synthetic.main.activity_player.playerView

/**
 * @author eneim (2018/08/08).
 */
class PlayerActivity : BaseActivity() {

  companion object {
    private const val EXTRA_INIT_DATA = "kohii::player::init_data"
    private const val EXTRA_REBINDER = "kohii::player::rebinder"

    fun createIntent(
      context: Context,
      initData: InitData,
      rebinder: Rebinder
    ): Intent {
      val extras = Bundle().also {
        it.putParcelable(EXTRA_INIT_DATA, initData)
        it.putParcelable(EXTRA_REBINDER, rebinder)
      }
      return Intent(context, PlayerActivity::class.java).also {
        it.putExtras(extras)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_player)

    val extras = intent?.extras
    val initData = extras?.getParcelable<InitData>(EXTRA_INIT_DATA)
    val rebinder = extras?.getParcelable<Rebinder>(EXTRA_REBINDER)

    if (rebinder != null && initData != null) {
      val displaySize = Point().apply {
        this@PlayerActivity.windowManager.defaultDisplay.getSize(this)
      }

      if (displaySize.y * initData.aspectRatio >= displaySize.x) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
      } else {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
      }

      val kohii = Master[this]
      kohii.register(this)
          .attach(playerContainer)
      rebinder.bind(kohii, this.playerView)
    } else finish()
  }
}
