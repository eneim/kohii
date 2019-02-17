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

package kohii.v1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenStateReceiver(val kohii: Kohii) : BroadcastReceiver() {
  override fun onReceive(
    context: Context?,
    intent: Intent?
  ) {
    if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {
      // TODO pause all?
    } else if (intent?.action.equals(Intent.ACTION_SCREEN_ON)) {
      // TODO restart all?
    }
  }
}