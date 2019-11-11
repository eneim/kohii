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

package kohii.core

import android.view.View
import android.view.ViewGroup
import androidx.core.view.contains
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ui.PlayerView

internal class LazyPlayback<CONTAINER : ViewGroup>(
  manager: Manager,
  host: Host<*>,
  config: Config,
  container: CONTAINER
) : Playback<CONTAINER>(manager, host, config, container) {

  override fun <RENDERER : Any> onAttachRenderer(renderer: RENDERER?) {
    if (renderer is View) {
      require(container !== renderer)
      require(!container.contains(renderer))
      container.removeAllViews()
      container.addView(renderer)
    } else if (renderer is Fragment) {
      require(container.id != View.NO_ID)
      val fragmentManager: FragmentManager =
        when {
          manager.host is Fragment -> manager.host.childFragmentManager
          manager.host is FragmentActivity -> manager.host.supportFragmentManager
          else -> throw IllegalArgumentException("Need ${manager.host} to have FragmentManager")
        }

      val prev = fragmentManager.findFragmentById(container.id)
      if (prev !== renderer) {
        fragmentManager.commitNow { replace(container.id, renderer) }
      } else {
        // TODO check this
        renderer.view?.let {
          if (!container.contains(it)) {
            val parent = it.parent
            if (parent is ViewGroup) parent.removeView(it)
            container.removeAllViews()
            container.addView(it)
          }
        }
      }
    }

    if (renderer is PlayerView && config.controller is ControlDispatcher) {
      renderer.setControlDispatcher(config.controller)
      renderer.useController = true
    }
  }

  override fun <RENDERER : Any> onDetachRenderer(renderer: RENDERER?) {
    if (renderer is View) {
      require(container !== renderer)
      require(container.contains(renderer))
      container.removeView(renderer)
    } else if (renderer is Fragment) {
      require(container.id != View.NO_ID)
      val fragmentManager: FragmentManager =
        when {
          manager.host is Fragment -> manager.host.childFragmentManager
          manager.host is FragmentActivity -> manager.host.supportFragmentManager
          else -> throw IllegalArgumentException("Need ${manager.host} to have FragmentManager")
        }
      fragmentManager.commitNow { remove(renderer) }
    }

    if (renderer is PlayerView && config.controller is ControlDispatcher) {
      renderer.setControlDispatcher(null)
      renderer.useController = false
    }
  }
}
