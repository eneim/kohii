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
import androidx.fragment.app.commit
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStyle.CHROMELESS
import com.google.android.youtube.player.YouTubePlayer.Provider
import kohii.v1.sample.common.BaseActivity
import kohii.v1.yt1.YouTubePlayerFragment

class YouTubeActivity : BaseActivity() {

  lateinit var playerFragment: YouTubePlayerFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_youtube)

    val existing = supportFragmentManager.findFragmentById(R.id.youtubeFrame)
    if (existing == null || existing !is YouTubePlayerFragment) {
      playerFragment = YouTubePlayerFragment.newInstance()
      supportFragmentManager.commit {
        replace(R.id.youtubeFrame, playerFragment)
      }
    } else {
      playerFragment = existing
    }
  }

  override fun onResume() {
    super.onResume()
    playerFragment.initialize(object : OnInitializedListener {
      override fun onInitializationSuccess(
        provider: Provider?,
        player: YouTubePlayer?,
        restored: Boolean
      ) {
        player?.apply {
          setShowFullscreenButton(false)
          setManageAudioFocus(true)
          setPlayerStyle(CHROMELESS)
          if (restored) play() else loadVideo("EOjq4OIWKqM")
        }
      }

      override fun onInitializationFailure(
        provider: Provider?,
        result: YouTubeInitializationResult?
      ) {
      }
    })
  }
}
