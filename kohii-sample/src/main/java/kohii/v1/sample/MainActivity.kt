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

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kohii.v1.Kohii
import kotlinx.android.synthetic.main.main_activity.playerView

class MainActivity : AppCompatActivity() {

  companion object {
    const val videoUrl = "https://storage.googleapis.com/spec-host/mio-material/assets/1MvJxcu1kd5TFR6c5IBhxjLueQzSZvVQz/m2-manifesto.mp4"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
//    if (savedInstanceState == null) {
//      supportFragmentManager.beginTransaction()
//          .replace(R.id.container, MainFragment.newInstance())
//          .commitNow()
//    }

    Kohii.with(this).setUp(Uri.parse(videoUrl)).asPlayable().bind(playerView)
  }
}
