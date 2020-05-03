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

package kohii.v1.core

/**
 * An observer to allow client to know when the [Playable] (defined by its tag)'s [Playback] is
 * changed. Client can use the [Manager.observe] method to register an observer.
 */
typealias PlayableObserver = (
  Any /* Playable Tag */,
  Playback? /* Previous Playback */,
  Playback? /* Next Playback */
) -> Unit

/**
 * Refer to values of [com.google.android.exoplayer2.C.NetworkType]
 */
typealias NetworkType = Int
