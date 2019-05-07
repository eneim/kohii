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

package kohii.v1.sample.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

@SuppressWarnings({ "WeakerAccess", "unused" }) //
public class Sources implements Parcelable {
  final String type;
  final List<String> mediaTypes;
  final String file;

  public Sources(String type, List<String> mediaTypes, String file) {
    this.type = type;
    this.mediaTypes = mediaTypes;
    this.file = file;
  }

  public String getType() {
    return type;
  }

  public List<String> getMediaTypes() {
    return mediaTypes;
  }

  public String getFile() {
    return file;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.type);
    dest.writeStringList(this.mediaTypes);
    dest.writeString(this.file);
  }

  protected Sources(Parcel in) {
    this.type = in.readString();
    this.mediaTypes = in.createStringArrayList();
    this.file = in.readString();
  }

  public static final Creator<Sources> CREATOR = new Creator<Sources>() {
    @Override public Sources createFromParcel(Parcel source) {
      return new Sources(source);
    }

    @Override public Sources[] newArray(int size) {
      return new Sources[size];
    }
  };
}
