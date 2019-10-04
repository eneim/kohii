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

// A Target definition for a specific Container type.
// Useful when use with LazyViewPlayback.
interface Target<CONTAINER : Any, RENDERER : Any> {

  val container: CONTAINER

  fun attachRenderer(renderer: RENDERER)

  // Returning true if the detachment did something meaningful,
  // For example: ViewGroup remove the PlayerView.
  fun detachRenderer(renderer: RENDERER): Boolean
}
