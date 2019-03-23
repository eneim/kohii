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

package kohii.v1.sample.ui.nested

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.core.util.Pair

class HolderStateEntry(
  first: Int?,
  second: Int?
) : Pair<Int, Int>(first, second), Parcelable {

  constructor(parcel: Parcel) : this(
      parcel.readInt(),
      parcel.readInt()
  )

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {}

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Creator<HolderStateEntry> {
    override fun createFromParcel(parcel: Parcel): HolderStateEntry {
      return HolderStateEntry(parcel)
    }

    override fun newArray(size: Int): Array<HolderStateEntry?> {
      return arrayOfNulls(size)
    }
  }
}
