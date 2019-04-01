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

package kohii.v1.sample.common

import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener

interface TransitionListenerAdapter : TransitionListener {

  override fun onTransitionTrigger(
    p0: MotionLayout?,
    p1: Int,
    p2: Boolean,
    p3: Float
  ) {
  }

  override fun onTransitionStarted(
    motionLayout: MotionLayout,
    startId: Int,
    endId: Int
  ) {
  }

  override fun onTransitionChange(
    motionLayout: MotionLayout,
    startId: Int,
    endId: Int,
    progress: Float
  ) {
  }

  override fun onTransitionCompleted(
    motionLayout: MotionLayout,
    currentId: Int
  ) {
  }
}
