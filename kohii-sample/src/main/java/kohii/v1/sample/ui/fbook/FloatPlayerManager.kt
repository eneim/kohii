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

package kohii.v1.sample.ui.fbook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.GravityCompat
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.Kohii
import kohii.v1.sample.R
import java.util.concurrent.atomic.AtomicBoolean

class FloatPlayerManager(val activity: Activity) {

  val kohii = Kohii[activity]
  val windowManager = activity.getSystemService(WINDOW_SERVICE) as WindowManager
  @SuppressLint("InflateParams")
  val floatView: View = LayoutInflater.from(activity)
      .inflate(R.layout.widget_float_player, null)

  val floatParams by lazy {
    val params = WindowManager.LayoutParams()
    @Suppress("DEPRECATION")
    params.type = if (VERSION.SDK_INT >= VERSION_CODES.O)
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
      WindowManager.LayoutParams.TYPE_PHONE

    params.format = PixelFormat.TRANSLUCENT
    params.flags = params.flags or (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

    params.gravity = GravityCompat.END or Gravity.BOTTOM
    params.width = WindowManager.LayoutParams.WRAP_CONTENT
    params.height = WindowManager.LayoutParams.WRAP_CONTENT
    return@lazy params
  }

  private val displayRect = Rect()
  private val windowRect = Rect()

  private var viewTouchConsumedByMove = false
  private var floatViewLastX: Int = 0
  private var floatViewLastY: Int = 0
  private var floatViewFirstX: Int = 0
  private var floatViewFirstY: Int = 0

  val floatViewOnTouchListener = View.OnTouchListener { _, event ->
    val params = floatParams
    Log.i("Kohii::Fb", "float params: ${params.x}, ${params.y}")
    val totalDeltaX = floatViewLastX - floatViewFirstX
    val totalDeltaY = floatViewLastY - floatViewFirstY

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        floatViewLastX = event.rawX.toInt()
        floatViewLastY = event.rawY.toInt()
        floatViewFirstX = floatViewLastX
        floatViewFirstY = floatViewLastY
      }
      MotionEvent.ACTION_UP -> {
        // Snap to one corner
        val display = floatView.display
        if (display != null) {
          display.getRectSize(displayRect)
          GravityCompat.applyDisplay(
              params.gravity, displayRect, windowRect, floatView.layoutDirection
          )
        }
        Log.w("Kohii::Fb", "window rect: $windowRect")
      }
      MotionEvent.ACTION_MOVE -> {
        val deltaX = event.rawX.toInt() - floatViewLastX
        val deltaY = event.rawY.toInt() - floatViewLastY
        floatViewLastX = event.rawX.toInt()
        floatViewLastY = event.rawY.toInt()
        if (Math.abs(totalDeltaX) >= 5 || Math.abs(totalDeltaY) >= 5) {
          if (event.pointerCount == 1) {
            params.x -= deltaX
            params.y -= deltaY
            viewTouchConsumedByMove = true
            windowManager.updateViewLayout(floatView, params)
          } else {
            viewTouchConsumedByMove = false
          }
        } else {
          viewTouchConsumedByMove = false
        }
      }
      else -> {
      }
    }
    viewTouchConsumedByMove
  }

  val container by lazy { floatView.findViewById(R.id.playerContainer) as ViewGroup }
  val playerView by lazy { floatView.findViewById(R.id.playerView) as PlayerView }

  val floating = AtomicBoolean(false)

  inline fun openFloatPlayer(crossinline callback: (PlayerView) -> Unit) {
    if (floating.compareAndSet(false, true)) {
      activity.runOnUiThread {
        if (!activity.isFinishing) {
          floatView.setOnTouchListener(floatViewOnTouchListener)
          windowManager.addView(floatView, floatParams)
          kohii.register(activity, container)
          callback(this.playerView)
        }
      }
    }
  }

  inline fun closeFloatPlayer(crossinline callback: () -> Unit) {
    if (floating.compareAndSet(true, false)) {
      activity.runOnUiThread {
        floatView.setOnTouchListener(null)
        callback()
        floatView.animate()
            .alpha(0F)
            .setDuration(150)
            .withEndAction { windowManager.removeView(floatView) }
            .start()
      }
    }
  }
}
