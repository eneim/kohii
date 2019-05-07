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

package kohii.v1.sample.common

import android.app.Activity
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import android.view.View

/**
 * @author eneim (2018/07/30).
 */
fun Int.toPixel(resources: Resources): Int {
  return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
      resources.displayMetrics
  )
      .toInt()
}

// Improved (or Degraded?) version of ktx.doOnNextLayout
inline fun <reified T : View> View.doOnNextLayoutAs(crossinline action: (view: T) -> Unit) {
  addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
    override fun onLayoutChange(
      view: View,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      oldLeft: Int,
      oldTop: Int,
      oldRight: Int,
      oldBottom: Int
    ) {
      view.removeOnLayoutChangeListener(this)
      action(view as T)
    }
  })
}

fun Activity.inMultiWindow(): Boolean {
  return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && this.isInMultiWindowMode
}

fun Activity.getDisplayPoint(): Point {
  return Point().also {
    this.windowManager.defaultDisplay.getSize(it)
  }
}

fun Activity.isLandscape(): Boolean {
  val display = this.windowManager.defaultDisplay
  val realSize = Point().let {
    display.getRealSize(it)
    it
  }
  return Point().let {
    display.getSize(it)
    it.x >= it.y || (inMultiWindow() && it.y <= realSize.y * 0.5)
  }
}
