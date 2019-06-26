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

package kohii.internal

import android.graphics.Bitmap
import android.os.AsyncTask
import com.google.android.exoplayer2.ui.PlayerNotificationManager.BitmapCallback
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class BitmapAsyncTask(
  private val lazyBitmap: Future<Bitmap?>,
  private val bitmapCallback: BitmapCallback?
) : AsyncTask<Void, Void, Bitmap?>() {

  companion object {
    private val threadFactory = object : ThreadFactory {
      private val mCount = AtomicInteger(1)

      override fun newThread(r: Runnable): Thread {
        return Thread(r, "BitmapAsyncTask #" + mCount.getAndIncrement())
      }
    }

    internal val SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor(threadFactory)
  }

  override fun doInBackground(vararg params: Void?): Bitmap? {
    return lazyBitmap.get()
  }

  override fun onPostExecute(result: Bitmap?) {
    super.onPostExecute(result)
    bitmapCallback?.onBitmap(result)
  }
}
