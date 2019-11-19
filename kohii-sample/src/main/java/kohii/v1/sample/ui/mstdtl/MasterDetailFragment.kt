/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
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

package kohii.v1.sample.ui.mstdtl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.commit
import kohii.v1.core.Master
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.ui.pagers.GridContentFragment
import kohii.v1.sample.ui.pagers.ViewPager1WithFragmentsFragment
import kotlinx.android.synthetic.main.fragment_master_detail.container

/**
 * @author eneim (2018/07/13).
 */
@Keep
class MasterDetailFragment : BaseFragment() {

  companion object {
    fun newInstance() = MasterDetailFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_master_detail, container, false)
  }

  lateinit var kohii: Master

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    kohii = Master[this]
    kohii.register(this)
        .attach(container)

    if (savedInstanceState == null) {
      childFragmentManager.commit {
        replace(R.id.topContainer, ViewPager1WithFragmentsFragment.newInstance())
        replace(R.id.bottomContainer, GridContentFragment.newInstance())
      }
    }

    childFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentLifecycleCallbacks() {
      override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
      ) {
        if (f.id == R.id.topContainer) {
          kohii.stick(f.viewLifecycleOwner)
        }
      }

      override fun onFragmentViewDestroyed(
        fm: FragmentManager,
        f: Fragment
      ) {
        if (f.id == R.id.topContainer) {
          kohii.unstick(f.viewLifecycleOwner)
        }
      }
    }, false)
  }
}
