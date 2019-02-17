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

import android.net.Uri
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.sample.R
import kohii.v1.sample.svg.GlideApp

/**
 * For DataBinding
 *
 * @author eneim (2018/07/15).
 */
@Suppress("unused")
@BindingAdapter("backdrop")
fun setBackdrop(
  view: ImageView,
  url: String
) {
  GlideApp.with(view)
      .load(Uri.parse(url))
      .into(view)
}

@BindingAdapter("video")
fun setVideo(
  view: PlayerView,
  video: Video
) {
  (view.findViewById(R.id.exo_content_frame) as? AspectRatioFrameLayout)
      ?.setAspectRatio(video.width / video.height)

//  Kohii[view.context].setUp(MediaItem(video.url, "mp4"))
//      .copy(
//          tag = "${video.javaClass.canonicalName}::${video.url}",
//          prefetch = true,
//          repeatMode = Player.REPEAT_MODE_ONE
//      )
//      .asPlayable()
//      .bind(view, cb = null)
  ViewCompat.setTransitionName(view, video.url)
}