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

package kohii.common

import android.view.ActionMode
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

internal open class WindowCallbackWrapper(private val origin: Window.Callback) : Window.Callback {

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    return origin.dispatchKeyEvent(event)
  }

  override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean {
    return origin.dispatchKeyShortcutEvent(event)
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    return origin.dispatchTouchEvent(event)
  }

  override fun dispatchTrackballEvent(event: MotionEvent): Boolean {
    return origin.dispatchTrackballEvent(event)
  }

  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
    return origin.dispatchGenericMotionEvent(event)
  }

  override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
    return origin.dispatchPopulateAccessibilityEvent(event)
  }

  override fun onCreatePanelView(featureId: Int): View? {
    return origin.onCreatePanelView(featureId)
  }

  override fun onCreatePanelMenu(
    featureId: Int,
    menu: Menu
  ): Boolean {
    return origin.onCreatePanelMenu(featureId, menu)
  }

  override fun onPreparePanel(
    featureId: Int,
    view: View,
    menu: Menu
  ): Boolean {
    return origin.onPreparePanel(featureId, view, menu)
  }

  override fun onMenuOpened(
    featureId: Int,
    menu: Menu
  ): Boolean {
    return origin.onMenuOpened(featureId, menu)
  }

  override fun onMenuItemSelected(
    featureId: Int,
    item: MenuItem
  ): Boolean {
    return origin.onMenuItemSelected(featureId, item)
  }

  override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams) {
    origin.onWindowAttributesChanged(attrs)
  }

  override fun onContentChanged() {
    origin.onContentChanged()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    origin.onWindowFocusChanged(hasFocus)
  }

  override fun onAttachedToWindow() {
    origin.onAttachedToWindow()
  }

  override fun onDetachedFromWindow() {
    origin.onDetachedFromWindow()
  }

  override fun onPanelClosed(
    featureId: Int,
    menu: Menu
  ) {
    origin.onPanelClosed(featureId, menu)
  }

  @RequiresApi(23)
  override fun onSearchRequested(searchEvent: SearchEvent): Boolean {
    return origin.onSearchRequested(searchEvent)
  }

  override fun onSearchRequested(): Boolean {
    return origin.onSearchRequested()
  }

  override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode? {
    return origin.onWindowStartingActionMode(callback)
  }

  @RequiresApi(23)
  override fun onWindowStartingActionMode(
    callback: ActionMode.Callback,
    type: Int
  ): ActionMode? {
    return origin.onWindowStartingActionMode(callback, type)
  }

  override fun onActionModeStarted(mode: ActionMode) {
    origin.onActionModeStarted(mode)
  }

  override fun onActionModeFinished(mode: ActionMode) {
    origin.onActionModeFinished(mode)
  }

  @RequiresApi(24)
  override fun onProvideKeyboardShortcuts(
    data: List<KeyboardShortcutGroup>,
    menu: Menu?,
    deviceId: Int
  ) {
    origin.onProvideKeyboardShortcuts(data, menu, deviceId)
  }

  @RequiresApi(26)
  override fun onPointerCaptureChanged(hasCapture: Boolean) {
    origin.onPointerCaptureChanged(hasCapture)
  }
}