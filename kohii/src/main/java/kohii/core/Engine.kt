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

package kohii.core

import android.net.Uri
import androidx.core.net.toUri
import kohii.media.Media
import kohii.media.MediaItem

// TODO may need RendererProvider<RENDERER> in Engine instance? Problem: we want to keep Engine
//  instance at global/Application scope. Since Renderer must live in Activity scope, keeping a
//  RendererProvider in Engine may cause a memory leak if not well designed.

// TODO support manual Playback creation in Engine instance.
// TODO need to support sub-type of a Renderer type. Eg: PlayerView and classes that extend it.
abstract class Engine<RENDERER : Any>(
  val master: Master,
  internal val playableCreator: PlayableCreator<RENDERER>
) {

  // TODO implement the method below.
  // abstract fun <T> supportRendererType(type: Class<T>): Boolean

  open fun setUp(media: Media): Binder<RENDERER> = Binder(this, media)

  open fun setUp(uri: Uri) = setUp(MediaItem(uri))

  open fun setUp(url: String) = setUp(url.toUri())
}
