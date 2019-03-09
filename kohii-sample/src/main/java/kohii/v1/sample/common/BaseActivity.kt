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

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * @author eneim (2018/08/05).
 */
abstract class BaseActivity : AppCompatActivity() {

  private var logTag: String = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logTag = "Kohii::App:${javaClass.simpleName}"
    Log.d(logTag, "onCreate() called, state: $savedInstanceState")
  }

  override fun onStart() {
    super.onStart()
    Log.d(logTag, "onStart() called")
  }

  override fun onStop() {
    super.onStop()
    Log.d(logTag, "onStop() called")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(logTag, "onDestroy() called")
  }
}