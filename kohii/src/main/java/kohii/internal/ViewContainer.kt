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

import android.view.View
import androidx.core.view.ViewCompat
import kohii.v1.Container
import kohii.v1.Container.Companion.changed
import kohii.v1.PlaybackManager
import java.util.WeakHashMap

abstract class ViewContainer<V : View>(
  override val container: V,
  open val manager: PlaybackManager
) : Container, View.OnAttachStateChangeListener,
    View.OnLayoutChangeListener {

  private val targets = WeakHashMap<View, Any>()

  override fun <T> attachTarget(target: T) {
    if (target is View && !targets.containsKey(target)) {
      if (ViewCompat.isAttachedToWindow(target)) {
        this.onViewAttachedToWindow(target)
      }
      target.addOnAttachStateChangeListener(this)
      targets[target] = Container.PRESENT
    }
  }

  override fun <T> detachTarget(target: T) {
    if (target is View && targets.containsKey(target)) {
      target.removeOnAttachStateChangeListener(this)
      targets.remove(target)
    }
  }

  override fun onViewAttachedToWindow(v: View?) {
    if (v != null) {
      manager.onTargetActive(v)
      v.addOnLayoutChangeListener(this)
    }
  }

  override fun onViewDetachedFromWindow(v: View?) {
    if (v != null) {
      v.removeOnLayoutChangeListener(this)
      manager.onTargetInActive(v)
    }
  }

  override fun onLayoutChange(
    v: View?,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if (v != null && changed(left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)) {
      manager.onTargetUpdated(v)
    }
  }

  override fun toString(): String {
    return "${container.javaClass.simpleName}::${Integer.toHexString(hashCode())}"
  }
}
