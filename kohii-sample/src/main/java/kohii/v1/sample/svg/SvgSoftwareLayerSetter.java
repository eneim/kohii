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

package kohii.v1.sample.svg;

import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

/**
 * Listener which updates the {@link ImageView} to be software rendered, because
 * {@link com.caverock.androidsvg.SVG SVG}/{@link android.graphics.Picture Picture} can't render on
 * a hardware backed {@link android.graphics.Canvas Canvas}.
 */
public class SvgSoftwareLayerSetter implements RequestListener<PictureDrawable> {

  @Override
  public boolean onLoadFailed(GlideException e, Object model, Target<PictureDrawable> target,
      boolean isFirstResource) {
    ImageView view = ((ImageViewTarget<?>) target).getView();
    view.setLayerType(ImageView.LAYER_TYPE_NONE, null);
    return false;
  }

  @Override public boolean onResourceReady(PictureDrawable resource, Object model,
      Target<PictureDrawable> target, DataSource dataSource, boolean isFirstResource) {
    ImageView view = ((ImageViewTarget<?>) target).getView();
    view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
    return false;
  }
}