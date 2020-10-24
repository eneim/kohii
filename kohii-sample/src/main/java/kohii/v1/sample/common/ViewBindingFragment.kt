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

package kohii.v1.sample.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class ViewBindingFragment<V : ViewBinding>(
  val bindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> V
) : BaseFragment() {

  private var _binding: V? = null

  val binding: V? get() = _binding

  fun requireBinding(): V = checkNotNull(_binding)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = bindingCreator(inflater, container, false)
    return requireBinding().root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
