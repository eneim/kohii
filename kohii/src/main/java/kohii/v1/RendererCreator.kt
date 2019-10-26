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

package kohii.v1

import kohii.media.Media

// In theory, instance of this interface doesn't need to be scoped in Activity lifecycle.
interface RendererCreator<RENDERER : Any> {

  // Return renderer type for a Media object, as an integer.
  // This value will be used to cache the renderer used for this Media object.
  // Same type Media object will use same type renderer.
  fun getMediaType(media: Media): Int = R.layout.kohii_player_surface_view

  fun getContainerType(container: Any): Int = 0

  fun <CONTAINER : Any> createRenderer(
    playback: Playback<RENDERER>,
    container: CONTAINER,
    type: Int
  ): RENDERER
}
