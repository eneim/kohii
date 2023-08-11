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

package kohii.v1.sample.tiktok.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kohii.v1.sample.tiktok.R

class DashboardFragment : Fragment() {

  private lateinit var dashboardViewModel: DashboardViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
    val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
    val textView: TextView = root.findViewById(R.id.text_dashboard)
    dashboardViewModel.text.observe(
      viewLifecycleOwner,
      Observer {
        textView.text = it
      }
    )
    return root
  }
}
