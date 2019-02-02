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

package kohii.v1.sample.ui.reuse.data;

import com.squareup.moshi.Json;
import java.util.List;

@SuppressWarnings({ "WeakerAccess", "unused" }) //
public class Video {

  @Json(name = "feed_instance_id") final String feedInstanceId;
  final String title;
  final String kind;
  final List<Playlist> playlist;
  final String description;

  public Video(String feedInstanceId, String title, String kind,
      List<Playlist> playlist, String description) {
    this.feedInstanceId = feedInstanceId;
    this.title = title;
    this.kind = kind;
    this.playlist = playlist;
    this.description = description;
  }

  public String getFeedInstanceId() {
    return feedInstanceId;
  }

  public String getTitle() {
    return title;
  }

  public String getKind() {
    return kind;
  }

  public List<Playlist> getPlaylist() {
    return playlist;
  }

  public String getDescription() {
    return description;
  }
}