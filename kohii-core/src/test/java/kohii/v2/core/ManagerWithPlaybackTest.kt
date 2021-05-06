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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class ManagerWithPlaybackTest {

  @Test
  fun `test Manager#addPlayback triggers Bucket#addContainer`() {
    val manager = spy(Manager())

    val bucket: Bucket = mock()
    val container: Any = mock()
    val playback: Playback = mock {
      on { this.container } doReturn container
      on { this.bucket } doReturn bucket
    }

    manager.addPlayback(playback)
    verify(bucket).addContainer(container)
    verify(playback).performAdd()
    verify(manager).refresh()
  }

  @Test
  fun `test Manager does not add Playback more than once`() {
    val manager = spy(Manager())

    val bucket: Bucket = mock()
    val container: Any = mock()
    val playback: Playback = mock {
      on { this.container } doReturn container
      on { this.bucket } doReturn bucket
    }

    try {
      manager.addPlayback(playback)
      manager.addPlayback(playback)
      fail("A Playback can be added twice.")
    } catch (error: Throwable) {
      assertTrue { error is IllegalArgumentException }
    }
  }

  @Test
  fun `test Manager does not allow many Playbacks for same container`() {
    val manager = spy(Manager())

    val bucket: Bucket = mock()
    val container: Any = mock()
    val playback1: Playback = mock {
      on { this.container } doReturn container
      on { this.bucket } doReturn bucket
    }

    val playback2: Playback = mock {
      on { this.container } doReturn container
      on { this.bucket } doReturn bucket
    }

    try {
      manager.addPlayback(playback1)
      manager.addPlayback(playback2)
      fail("Different Playbacks using same container are allowed.")
    } catch (error: Throwable) {
      assertTrue { error is IllegalArgumentException }
    }
  }

  @Test
  fun `test Manager#removePlayback triggers Bucket#removeContainer`() {
    val manager = spy(Manager())

    val bucket: Bucket = mock()
    val container: Any = mock()
    val playback: Playback = mock {
      on { this.container } doReturn container
      on { this.bucket } doReturn bucket
    }

    manager.addPlayback(playback)
    manager.removePlayback(playback)
    verify(bucket).removeContainer(container)
    verify(playback, times(1)).performRemove()
  }
}
