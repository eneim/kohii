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

package kohii.v1.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.FragmentMainBinding
import kotlinx.android.synthetic.main.fragment_main.intro

interface Presenter {

  fun onItemClick(target: String) {}
}

class MainFragment : BaseFragment(), Presenter {

  companion object {
    fun newInstance() = MainFragment()
  }

  private var binding: FragmentMainBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = (DataBindingUtil.inflate(
        inflater,
        R.layout.fragment_main,
        container,
        false
    ) as FragmentMainBinding)
    return binding!!.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    intro.text = HtmlCompat.fromHtml(getString(R.string.lib_intro), FROM_HTML_MODE_COMPACT)
  }

  override fun onStart() {
    super.onStart()
    binding?.presenter = this
  }

  override fun onStop() {
    super.onStop()
    binding?.presenter = null
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }

  override fun onItemClick(target: String) {
    super.onItemClick(target)
    val clazz = Class.forName(target)
    val fragment = (clazz.newInstance() as Fragment)

    fragmentManager?.also {
      it.beginTransaction()
          .setReorderingAllowed(true) // Optimize for shared element transition
          .replace(R.id.fragmentContainer, fragment, fragment.javaClass.canonicalName)
          .addToBackStack(null)
          .commit()
    }
  }
}
