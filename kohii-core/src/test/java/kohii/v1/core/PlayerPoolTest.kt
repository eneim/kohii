/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
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

package kohii.v1.core

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kohii.v1.media.Media
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PlayerPoolTest {

  @Test
  fun `(All) Creating PlayerPool of non-positive size throws IllegalArgumentException`() {
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    try {
      object : PlayerPool<Any>(0) {
        override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
        override fun destroyPlayer(player: Any) = Unit
      }
      fail("Creating PlayerPool of non-positive size must throw IllegalArgumentException")
    } catch (er: Exception) {
      assert(er is IllegalArgumentException)
    }

    try {
      object : PlayerPool<Any>(-1) {
        override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
        override fun destroyPlayer(player: Any) = Unit
      }
      fail("Creating PlayerPool of non-positive size must throw IllegalArgumentException")
    } catch (er: Exception) {
      assert(er is IllegalArgumentException)
    }
  }

  @Test
  fun `(All) PlayerPool clear() must call destroyPlayer if not empty`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    playerPool.putPlayer(mockMedia, player)
    playerPool.clear()

    verify(mockPool).destroyPlayer(eq(player))
  }

  @Test
  fun `(All) PlayerPool putPlayer of same instance throws IllegalStateException`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()

    val playerPool = object : PlayerPool<Any>(poolSize = 1) {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    try {
      playerPool.putPlayer(mockMedia, mockPlayer)
      playerPool.putPlayer(mockMedia, mockPlayer)
      fail("Releasing same instance twice must throw IllegalStateException")
    } catch (er: Exception) {
      assert(er is IllegalStateException)
    }
  }

  @Test
  fun `(All) Full PlayerPool putPlayer must call destroyPlayer`() {
    val mockMedia: Media = mock()
    val mockPlayer1: Any = mock()
    val mockPlayer2: Any = mock()
    val mockPool: PlayerPool<Any> = mock()

    val playerPool = object : PlayerPool<Any>(poolSize = 1) {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    playerPool.putPlayer(mockMedia, mockPlayer1)
    playerPool.putPlayer(mockMedia, mockPlayer2)

    verify(mockPool).destroyPlayer(eq(mockPlayer2))
  }

  @Test
  fun `(All) PlayerPool first getPlayer must call createPlayer`() {
    // Mock a delegation that creates non-null Player.
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.getPlayer(any())).thenReturn(mockPlayer)

    val mockMedia: Media = mock()

    val playerPool = object : PlayerPool<Any>() {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = Unit
    }

    playerPool.getPlayer(mockMedia)
    verify(mockPool, times(1)).createPlayer(eq(mockMedia))
  }

  @Test
  fun `(Default) Not full PlayerPool putPlayer must return true`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>(100) {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = Unit
      override fun resetPlayer(player: Any) = mockPool.resetPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    val released = playerPool.putPlayer(mockMedia, player)
    assert(released) {
      "putPlayer must return true"
    }
  }

  @Test
  fun `(Default) Not full PlayerPool putPlayer must call resetPlayer`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = Unit
      override fun resetPlayer(player: Any) = mockPool.resetPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    if (playerPool.putPlayer(mockMedia, player)) {
      verify(mockPool, times(1)).resetPlayer(eq(mockPlayer))
    }
  }

  @Test
  fun `(Default) PlayerPool must recycle Player instance`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = Unit
    }

    val player = playerPool.getPlayer(mockMedia)
    playerPool.putPlayer(mockMedia, player)
    playerPool.getPlayer(mockMedia)

    verify(mockPool, times(1)).createPlayer(eq(mockMedia))
  }

  @Test
  fun `(None-recycling) PlayerPool getPlayer always call createPlayer`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun recyclePlayerForMedia(media: Media): Boolean = false
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    playerPool.putPlayer(mockMedia, player)
    playerPool.getPlayer(mockMedia)
    verify(mockPool, times(2)).createPlayer(eq(mockMedia))
  }

  @Test
  fun `(None-recycling) PlayerPool putPlayer return false`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun recyclePlayerForMedia(media: Media): Boolean = false
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    val released = playerPool.putPlayer(mockMedia, player)
    assert(!released) {
      "putPlayer must return false"
    }
  }

  @Test
  fun `(None-recycling) PlayerPool putPlayer must call destroyPlayer`() {
    val mockMedia: Media = mock()
    val mockPlayer: Any = mock()
    val mockPool: PlayerPool<Any> = mock()
    whenever(mockPool.createPlayer(any())).thenReturn(mockPlayer)

    val playerPool = object : PlayerPool<Any>() {
      override fun recyclePlayerForMedia(media: Media): Boolean = false
      override fun createPlayer(media: Media): Any = mockPool.createPlayer(media)
      override fun destroyPlayer(player: Any) = mockPool.destroyPlayer(player)
    }

    val player = playerPool.getPlayer(mockMedia)
    playerPool.putPlayer(mockMedia, player)

    verify(mockPool).destroyPlayer(eq(player))
  }
}
