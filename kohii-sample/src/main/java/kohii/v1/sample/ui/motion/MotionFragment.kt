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

package kohii.v1.sample.ui.motion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import kohii.v1.sample.R
import kohii.v1.sample.common.BaseFragment
import kohii.v1.sample.databinding.FragmentMotionBinding
import kohii.v1.sample.ui.player.InitData
import kohii.v1.sample.ui.player.PlayerActivity

/**
 * @author eneim (2018/07/15).
 */
@Keep
class MotionFragment : BaseFragment(), Presenter {

  companion object {
    fun newInstance() = MotionFragment()
  }

  private var binding: FragmentMotionBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = (DataBindingUtil.inflate(
        inflater,
        R.layout.fragment_motion,
        container,
        false
    ) as FragmentMotionBinding).also {
      it.motion = Motion()
      it.lifecycleOwner = this
    }
    return binding!!.root
  }

  override fun onStart() {
    super.onStart()
    binding?.presenter = this
  }

  override fun onStop() {
    super.onStop()
    binding?.presenter = null
  }

  override fun onVideoClick(
    container: View,
    video: Video
  ) {
    startActivity(
        PlayerActivity.createIntent(
            requireContext(),
            InitData(
                tag = "${video.javaClass.canonicalName}::${video.url}",
                aspectRatio = video.width / video.height
            )
        )
    )
  }

  override fun requireLifecycleOwner(): LifecycleOwner {
    return this.viewLifecycleOwner
  }
}