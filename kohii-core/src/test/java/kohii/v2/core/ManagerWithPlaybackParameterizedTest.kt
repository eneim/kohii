/*
 * Copyright (c) 2021 Nam Nguyen, nam@ene.im
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

package kohii.v2.core

import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import kotlin.test.Test

@RunWith(ParameterizedRobolectricTestRunner::class)
class ManagerWithPlaybackParameterizedTest(
  private val playbackStarted: Boolean,
  private val playbackResumed: Boolean
) {

  init {
    if (!playbackStarted) require(!playbackResumed)
  }

  private val bucket: Bucket = mock()
  private val container: Any = mock()

  private val playback: Playback = mock {
    on { this.bucket } doReturn bucket
    on { this.container } doReturn container
    on { this.isStarted } doReturn playbackStarted
    on { this.isResumed } doReturn playbackResumed
  }

  @Test
  fun `test Manager#removePlayback with Playback callbacks`() {
    val manager = Manager()

    manager.addPlayback(playback)
    verify(playback).performAdd()

    manager.removePlayback(playback)

    if (playbackResumed) verify(playback).performPause()
    if (playbackStarted) verify(playback).performStop()
    verify(playback).performRemove()
  }

  companion object {
    @JvmStatic
    @Parameters(name = "Playback: started={0}, resumed={1}")
    fun scenarios(): List<Array<*>> = listOf(
        arrayOf(true, true),
        arrayOf(true, false),
        arrayOf(false, false)
    )
  }
}
