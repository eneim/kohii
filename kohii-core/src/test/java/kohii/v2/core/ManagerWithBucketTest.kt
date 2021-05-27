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
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class ManagerWithBucketTest {

  @Test
  fun `test Manager and or remove Bucket triggers callback`() {
    val manager = spy(Manager())
    val bucket: Bucket = mock()
    manager.addBucketInternal(bucket)
    verify(bucket).onAdd()

    manager.removeBucketInternal(bucket)
    verify(bucket).onRemove()

    verify(manager, times(2)).refresh()
  }

  @Test
  fun `test Manager do not add Bucket twice`() {
    val manager = spy(Manager())
    val bucket: Bucket = mock()
    manager.addBucketInternal(bucket)
    manager.addBucketInternal(bucket)
    verify(bucket, times(1)).onAdd()
    verify(manager, times(1)).refresh()
  }

  @Test
  fun `test Manager stick Bucket fails if not added`() {
    val manager = Manager()
    val bucket: Bucket = mock()

    try {
      manager.stickBucketInternal(bucket)
      fail("Manager can stick a Bucket that is not added.")
    } catch (error: Throwable) {
      assertTrue { error is IllegalArgumentException }
    }
  }

  @Test
  fun `test Manager add Buckets in correct order`() {
    val manager = Manager()
    val bucket1: Bucket = mock()
    val bucket2: Bucket = mock()
    val bucket3: Bucket = mock()

    val orderedBuckets = listOf(bucket1, bucket2, bucket3)

    manager.addBucketInternal(bucket1)
    manager.addBucketInternal(bucket2)
    manager.addBucketInternal(bucket3)

    manager.buckets.forEachIndexed { index, bucket ->
      assertEquals(expected = orderedBuckets[index], actual = bucket)
    }
  }

  @Test
  fun `test Manager stick Bucket to the head of Bucket queue`() {
    val manager = Manager()
    val bucket1: Bucket = mock()
    val bucket2: Bucket = mock()
    val bucket3: Bucket = mock()

    manager.addBucketInternal(bucket1)
    manager.addBucketInternal(bucket2)
    manager.addBucketInternal(bucket3)

    assertEquals(expected = bucket1, actual = manager.buckets.first)

    manager.stickBucketInternal(bucket2)
    assertEquals(expected = bucket2, actual = manager.stickyBucket)
    assertEquals(expected = bucket2, actual = manager.buckets.first)
  }

  @Test
  fun `test Manager unstickBucket works correctly`() {
    val manager = Manager()
    val bucket1: Bucket = mock()
    val bucket2: Bucket = mock()
    val bucket3: Bucket = mock()

    manager.addBucketInternal(bucket1)
    manager.addBucketInternal(bucket2)
    manager.addBucketInternal(bucket3)

    assertNull(manager.stickyBucket)

    // Unstick an unknown bucket, check that it has no effect to the Manager
    val bucket: Bucket = mock()
    manager.unstickBucketInternal(bucket)
    assertNull(manager.stickyBucket)
    assertTrue { manager.buckets.size == 3 }

    // Unstick the current head, make sure it is not removed.
    manager.unstickBucketInternal(bucket1)
    assertTrue { manager.buckets.size == 3 }
    assertEquals(expected = bucket1, actual = manager.buckets.first)

    // Stick then unstick, check the value of stickyBucket
    manager.stickBucketInternal(bucket2)
    assertEquals(expected = bucket2, actual = manager.stickyBucket)
    manager.unstickBucketInternal(bucket1)
    assertTrue { manager.buckets.size == 4 }
    assertEquals(expected = bucket2, actual = manager.stickyBucket)

    manager.unstickBucketInternal(bucket2)
    assertTrue { manager.buckets.size == 3 }
    assertNull(manager.stickyBucket)
  }
}
