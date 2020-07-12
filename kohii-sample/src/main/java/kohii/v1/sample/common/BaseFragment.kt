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

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import kohii.v1.sample.DemoApp

/**
 * @author eneim (2018/07/27).
 */
open class BaseFragment : Fragment() {

  companion object {
    const val videoUrl = "https://content.jwplatform.com/manifests/146UwF4L.m3u8"
    const val KEY_DEMO_ITEM = "kohii::container::demo::item"
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected val logTag by lazy { "Kohii::${javaClass.simpleName}" }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    Log.d(logTag, "onAttach() called: $context")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(logTag, "onCreate() called: $savedInstanceState")
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    Log.d(logTag, "onViewCreated() called")
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onStart() {
    super.onStart()
    Log.d(logTag, "onStart() called")
  }

  override fun onStop() {
    super.onStop()
    Log.d(logTag, "onStop() called")
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Log.d(logTag, "onDestroyView() called")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(logTag, "onDestroy() called")
  }

  override fun onDetach() {
    super.onDetach()
    Log.d(logTag, "onDetach() called")
  }
}

fun BaseFragment.getApp() = this.requireActivity().application as DemoApp

fun Fragment.checkOverlayPermission(): Boolean {
  if (Build.VERSION.SDK_INT < 23) {
    return true
  }
  return Settings.canDrawOverlays(this.requireContext())
}
