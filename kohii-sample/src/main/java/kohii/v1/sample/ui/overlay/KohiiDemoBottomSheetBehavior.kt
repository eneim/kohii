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

package kohii.v1.sample.ui.overlay

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

// This Behaviour is to prevent touch-through when the Bottom Sheet is expanded.
@Suppress("unused")
class KohiiDemoBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

  constructor() : super()
  constructor(
    context: Context,
    attrs: AttributeSet
  ) : super(context, attrs)

  override fun onInterceptTouchEvent(
    parent: CoordinatorLayout,
    child: V,
    event: MotionEvent
  ): Boolean {
    return super.onInterceptTouchEvent(parent, child, event) || super.getState() == STATE_EXPANDED
  }
}