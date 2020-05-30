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

package kohii.v1.internal

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kohii.v1.Experiment
import kohii.v1.core.Bucket
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.Playback

@Experiment
internal class DynamicFragmentRendererPlayback(
  manager: Manager,
  bucket: Bucket,
  container: ViewGroup,
  config: Config
) : Playback(manager, bucket, container, config) {

  init {
    check(tag != Master.NO_TAG) {
      "Using Fragment as Renderer requires a unique tag when setting up the Playable."
    }
  }

  private val fragmentManager: FragmentManager =
    when (manager.host) {
      is Fragment -> manager.host.childFragmentManager
      is FragmentActivity -> manager.host.supportFragmentManager
      else -> throw IllegalArgumentException("Need ${manager.host} to have a FragmentManager")
    }

  override fun onPlay() {
    playable?.setupRenderer(this)
    super.onPlay()
  }

  override fun onPause() {
    super.onPause()
    playable?.teardownRenderer(this)
  }

  override fun onAttachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is Fragment)
    // Learn from ViewPager2 FragmentPagerAdapter implementation.
    val view = renderer.view
    if (!renderer.isAdded && view != null) throw IllegalStateException("Bad state of Fragment.")
    if (renderer.isAdded) {
      return if (view == null) {
        scheduleAttachFragment(container, renderer)
        true
      } else {
        if (view.parent != null) {
          if (view.parent !== container) {
            addViewToContainer(view, container)
          }
          true
        } else {
          addViewToContainer(view, container)
          true
        }
      }
    } else {
      if (!fragmentManager.isStateSaved) {
        scheduleAttachFragment(container, renderer)
        fragmentManager.commitNow { add(renderer, tag.toString()) }
        return true
      } else {
        if (fragmentManager.isDestroyed) return false
        manager.lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
          override fun onStateChanged(
            source: LifecycleOwner,
            event: Event
          ) {
            if (fragmentManager.isStateSaved) return
            source.lifecycle.removeObserver(this)
            if (container.isAttachedToWindow) {
              onAttachRenderer(renderer)
            }
          }
        })
        return true
      }
    }
  }

  override fun onDetachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is Fragment)
    val view = renderer.view
    if (view != null) {
      val parent = view.parent
      if (parent != null && parent is ViewGroup) {
        parent.removeAllViews()
      }
    }

    if (!renderer.isAdded) return true
    if (fragmentManager.isStateSaved) return true
    fragmentManager.commitNow { remove(renderer) }
    return true
  }

  @Suppress("MemberVisibilityCanBePrivate")
  internal fun scheduleAttachFragment(
    container: ViewGroup,
    fragment: Fragment
  ) {
    fragmentManager.registerFragmentLifecycleCallbacks(object : FragmentLifecycleCallbacks() {
      override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
      ) {
        if (f === fragment) {
          fm.unregisterFragmentLifecycleCallbacks(this)
          addViewToContainer(v, container)
        }
      }
    }, false)
  }

  internal fun addViewToContainer(
    view: View,
    container: ViewGroup
  ) {
    if (container.childCount > 1) {
      throw IllegalStateException("Container must not have more than one children.")
    }

    if (view.parent === container) return
    if (container.childCount > 0) container.removeAllViews()

    (view.parent as? ViewGroup)?.removeView(view)

    container.addView(view)
  }
}
