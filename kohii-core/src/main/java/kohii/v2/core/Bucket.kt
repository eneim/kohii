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

import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import kohii.v2.internal.checkMainThread

/**
 * A class that manages a collection of container and takes care of their lifecycles. A bucket is
 * always managed by a [Manager].
 *
 * @property manager The [Manager] that manages this [Bucket].
 * @property root The root of this [Bucket].
 */
abstract class Bucket(
  val manager: Manager,
  val root: Any
) {

  /**
   * Called when this class is added to a [Manager].
   */
  @CallSuper
  @MainThread
  open fun onAdd(): Unit = checkMainThread()

  /**
   * Called when this class is removed from a [Manager].
   */
  @CallSuper
  @MainThread
  open fun onRemove(): Unit = checkMainThread()

  /**
   * Called when the [root] becomes available. For example if the root is a [View], it is when the
   * root is attached to the Window.
   */
  @CallSuper
  @MainThread
  open fun onStart(): Unit = checkMainThread()

  /**
   * Called when the [root] becomes unavailable. For example if the root is a [View], it is when the
   * root is detached to the Window.
   */
  @CallSuper
  @MainThread
  open fun onStop(): Unit = checkMainThread()

  /**
   * Overwrite to perform adding a container to this Bucket.
   */
  @CallSuper
  @MainThread
  open fun addContainer(container: Any) = checkMainThread()

  /**
   * Overwrite to perform removing a container to this Bucket.
   */
  @CallSuper
  @MainThread
  open fun removeContainer(container: Any) = checkMainThread()
}
