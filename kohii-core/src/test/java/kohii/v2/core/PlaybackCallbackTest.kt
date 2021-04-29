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
import kohii.v2.core.Playback.Callback
import kohii.v2.core.Playback.State
import kohii.v2.core.Playback.State.ADDED
import kohii.v2.core.Playback.State.CREATED
import kohii.v2.core.Playback.State.REMOVED
import kohii.v2.core.Playback.State.RESUMED
import kohii.v2.core.Playback.State.STARTED
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class PlaybackCallbackTest {

  @Test
  fun `test Callback onAdded is called and in correct order`() {
    val playback: Playback = newPlayback()
    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.onAdd()
    inOrder.verify(callback1).onAdded(playback)
    inOrder.verify(callback2).onAdded(playback)
  }

  @Test
  fun `test Callback onRemoved is called and in correct order`() {
    val playback: Playback = newPlayback()
    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.state = ADDED
    playback.onRemove()
    inOrder.verify(callback1).onRemoved(playback)
    inOrder.verify(callback2).onRemoved(playback)
  }

  @Test
  fun `test Callback onStarted is called and in correct order`() {
    val playback: Playback = newPlayback()
    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.state = ADDED
    playback.onStart()
    inOrder.verify(callback1).onStarted(playback)
    inOrder.verify(callback2).onStarted(playback)
  }

  @Test
  fun `test Callback onStopped is called and in correct order`() {
    val playback: Playback = newPlayback()
    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.state = STARTED
    playback.onStop()
    inOrder.verify(callback1).onStopped(playback)
    inOrder.verify(callback2).onStopped(playback)
  }

  @Test
  fun `test Callback onResumed is called and in correct order`() {
    val playback: Playback = newPlayback()
    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.state = STARTED
    playback.onResume()
    inOrder.verify(callback1).onResumed(playback)
    inOrder.verify(callback2).onResumed(playback)
  }

  @Test
  fun `test Callback onPaused is called and in correct order`() {
    val playback: Playback = newPlayback()

    val callback1: Callback = mock()
    playback.addCallback(callback1)

    val callback2: Callback = mock()
    playback.addCallback(callback2)

    val inOrder = inOrder(callback1, callback2)

    playback.state = RESUMED
    playback.onPause()
    inOrder.verify(callback1).onPaused(playback)
    inOrder.verify(callback2).onPaused(playback)
  }

  @Test
  fun `test Playback state after callback`() {
    testUsing(newPlayback()) { playback: Playback ->
      playback.assertStateEquals(CREATED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = CREATED
      playback.onAdd()
      playback.assertStateEquals(ADDED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = ADDED
      playback.onStart()
      playback.assertStateEquals(STARTED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = STARTED
      playback.onResume()
      playback.assertStateEquals(RESUMED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = RESUMED
      playback.onPause()
      playback.assertStateEquals(STARTED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = STARTED
      playback.onStop()
      playback.assertStateEquals(ADDED)
    }

    testUsing(newPlayback()) { playback ->
      playback.state = ADDED
      playback.onRemove()
      playback.assertStateEquals(REMOVED)
    }
  }

  @Test
  fun `test Playback state checking for callback`() {
    val states = State.values().toMutableList()

    testUsing(newPlayback()) { playback: Playback ->
      playback.assertStateEquals(CREATED)
    }

    // onAdd can be called only if the current state is CREATED
    for (state in states - CREATED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `CREATED`
        try {
          playback.onAdd()
          fail("onAdd() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${CREATED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onAdd()` fails when state=$state"
          )
        }
      }
    }

    // onRemove can be called only if the current state is ADDED
    for (state in states - ADDED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `ADDED`
        try {
          playback.onRemove()
          fail("onRemove() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${ADDED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onRemove()` fails when state=$state"
          )
        }
      }
    }

    // onStart can be called only if the current state is ADDED
    for (state in states - ADDED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `ADDED`
        try {
          playback.onStart()
          fail("onStart() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${ADDED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onStart()` fails when state=$state"
          )
        }
      }
    }

    // onStop can be called only if the current state is STARTED
    for (state in states - STARTED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `STARTED`
        try {
          playback.onStop()
          fail("onStop() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${STARTED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onStop()` fails when state=$state"
          )
        }
      }
    }

    // onResume can be called only if the current state is STARTED
    for (state in states - STARTED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `STARTED`
        try {
          playback.onResume()
          fail("onResume() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${STARTED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onResume()` fails when state=$state"
          )
        }
      }
    }

    // onPause can be called only if the current state is RESUMED
    for (state in states - RESUMED) {
      testUsing(newPlayback()) { playback ->
        playback.state = state // Anything but `RESUMED`
        try {
          playback.onPause()
          fail("onPause() is callable for unexpected state=$state")
        } catch (error: Throwable) {
          assertTrue { error is IllegalStateException }
          assertEquals(
              expected = "Expected Playback state: ${RESUMED}, Actual state: $state",
              actual = error.localizedMessage,
              message = "Checking condition for `Playback.onPause()` fails when state=$state"
          )
        }
      }
    }
  }

  private fun newPlayback(): Playback = object : Playback() {}

  private fun Playback.assertStateEquals(expected: State) = assertEquals(expected, state)

  // TODO(eneim): move this to a test utils class.
  private inline fun <T> testUsing(target: T, block: (T) -> Unit): Unit = block(target)
}
