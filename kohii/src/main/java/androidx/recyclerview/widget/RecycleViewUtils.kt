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

package androidx.recyclerview.widget

import android.view.View

internal object RecycleViewUtils {
  fun checkParams(
    recyclerView: RecyclerView,
    params: RecyclerView.LayoutParams?
  ): Boolean {
    // Must be
    // Either not bound (so params == null or its ViewHolder is null)
    // Or bound to the same RecyclerView with the Container
    return params?.mViewHolder == null || params.mViewHolder.mOwnerRecyclerView === recyclerView
  }

  fun fetchItemViewParams(target: View): RecyclerView.LayoutParams? {
    var params = target.layoutParams
    var parent = target.parent
    while (params != null && params !is RecyclerView.LayoutParams) {
      params = (parent as? View)?.layoutParams
      parent = parent.parent
    }
    return params as RecyclerView.LayoutParams?
  }
}