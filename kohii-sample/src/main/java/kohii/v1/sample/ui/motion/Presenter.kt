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

package kohii.v1.sample.ui.motion

import android.view.View
import kohii.core.Master

/**
 *
 * To bridge between [MotionFragment] and Data Binding event handling.
 *
 * @author eneim (2018/08/13).
 */
interface Presenter {

  fun onVideoClick(
    container: View,
    video: Video
  )

  fun requireProvider(): Master
}
