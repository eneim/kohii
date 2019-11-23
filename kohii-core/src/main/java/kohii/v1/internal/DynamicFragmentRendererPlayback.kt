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

import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import kohii.v1.Experiment
import kohii.v1.core.Bucket
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.Playback

// TODO when using with YouTube Player SDK, we want to use its restoring mechanism
//  (init with restore flag).  In that case, we may need to skip the detaching of Fragment.
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
    super.onPlay()
    playable?.considerRequestRenderer(this)
  }

  override fun onPause() {
    super.onPause()
    playable?.considerReleaseRenderer(this)
  }

  override fun onAttachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is Fragment)
    check(container.id != View.NO_ID)
    val existing = fragmentManager.findFragmentById(container.id)
    if (existing !== renderer) {
      fragmentManager.commitNow(allowStateLoss = true) {
        replace(container.id, renderer, tag.toString())
      }
    } else {
      val view = renderer.view
      if (view != null && !container.contains(view)) {
        val parent = view.parent
        if (parent is ViewGroup) parent.removeView(view)
        container.removeAllViews()
        container.addView(view)
      }
    }
    return true
  }

  override fun onDetachRenderer(renderer: Any?): Boolean {
    if (renderer == null) return false
    require(renderer is Fragment)
    check(container.id != View.NO_ID)
    if (renderer.tag == tag.toString()) {
      fragmentManager.commitNow(allowStateLoss = true) {
        remove(renderer)
      }
    }
    return true
  }
}
