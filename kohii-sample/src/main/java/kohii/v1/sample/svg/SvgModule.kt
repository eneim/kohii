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

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import java.io.InputStream

/**
 * Module for the SVG sample app.
 */
@GlideModule
class SvgModule : AppGlideModule() {

  override fun registerComponents(
    context: Context,
    glide: Glide,
    registry: Registry
  ) {
    registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
      .append(InputStream::class.java, SVG::class.java, SvgDecoder())
  }

  // Disable manifest parsing to avoid adding similar modules twice.
  override fun isManifestParsingEnabled(): Boolean {
    return false
  }
}
