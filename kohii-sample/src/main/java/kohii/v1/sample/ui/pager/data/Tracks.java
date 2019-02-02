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

package kohii.v1.sample.ui.pager.data;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("WeakerAccess") //
public class Tracks implements Parcelable {
  final String kind;
  final String file;

  public Tracks(String kind, String file) {
    this.kind = kind;
    this.file = file;
  }

  public String getKind() {
    return kind;
  }

  public String getFile() {
    return file;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.kind);
    dest.writeString(this.file);
  }

  protected Tracks(Parcel in) {
    this.kind = in.readString();
    this.file = in.readString();
  }

  public static final Creator<Tracks> CREATOR = new Creator<Tracks>() {
    @Override public Tracks createFromParcel(Parcel source) {
      return new Tracks(source);
    }

    @Override public Tracks[] newArray(int size) {
      return new Tracks[size];
    }
  };
}
