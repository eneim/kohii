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

import java.util.List;

@SuppressWarnings({ "SpellCheckingInspection", "unused", "WeakerAccess" }) //
public class Playlist {
  final String mediaid;
  final String description;
  final int pubdate;
  final String tags;
  final String image;
  final String title;
  final Variations variations;
  final List<Sources> sources;
  final List<Tracks> tracks;
  final String link;
  final int duration;

  public Playlist(String mediaid, String description, int pubdate, String tags,
      String image, String title, Variations variations,
      List<Sources> sources, List<Tracks> tracks, String link, int duration) {
    this.mediaid = mediaid;
    this.description = description;
    this.pubdate = pubdate;
    this.tags = tags;
    this.image = image;
    this.title = title;
    this.variations = variations;
    this.sources = sources;
    this.tracks = tracks;
    this.link = link;
    this.duration = duration;
  }

  public String getMediaid() {
    return mediaid;
  }

  public String getDescription() {
    return description;
  }

  public int getPubdate() {
    return pubdate;
  }

  public String getTags() {
    return tags;
  }

  public String getImage() {
    return image;
  }

  public String getTitle() {
    return title;
  }

  public Variations getVariations() {
    return variations;
  }

  public List<Sources> getSources() {
    return sources;
  }

  public List<Tracks> getTracks() {
    return tracks;
  }

  public String getLink() {
    return link;
  }

  public int getDuration() {
    return duration;
  }
}
