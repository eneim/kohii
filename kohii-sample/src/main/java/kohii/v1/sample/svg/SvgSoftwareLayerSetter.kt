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

package kohii.v1.sample.svg

import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target

/**
 * Listener which updates the [ImageView] to be software rendered, because
 * [SVG][com.caverock.androidsvg.SVG]/[Picture][android.graphics.Picture] can't render on
 * a hardware backed [Canvas][android.graphics.Canvas].
 */
class SvgSoftwareLayerSetter : RequestListener<PictureDrawable> {

  override fun onLoadFailed(
    e: GlideException?,
    model: Any,
    target: Target<PictureDrawable>,
    isFirstResource: Boolean
  ): Boolean {
    val view = (target as ImageViewTarget<*>).view
    view.setLayerType(ImageView.LAYER_TYPE_NONE, null)
    return false
  }

  override fun onResourceReady(
    resource: PictureDrawable,
    model: Any,
    target: Target<PictureDrawable>,
    dataSource: DataSource,
    isFirstResource: Boolean
  ): Boolean {
    val view = (target as ImageViewTarget<*>).view
    view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null)
    return false
  }
}