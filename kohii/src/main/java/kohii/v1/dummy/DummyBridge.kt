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

package kohii.v1.dummy

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater.from
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.ui.PlayerView
import kohii.media.PlaybackInfo
import kohii.media.VolumeInfo
import kohii.v1.Bridge
import kohii.v1.ErrorListener
import kohii.v1.Playable
import kohii.v1.PlayerEventListener
import kohii.v1.R
import kohii.v1.VolumeChangedListener

// Dummy, to show the 'plug & play' native of Kohii.
@Suppress("UseExpressionBody", "UNUSED_PARAMETER")
class DummyBridge(val context: Context) : Bridge {

  @SuppressLint("PrivateResource") //
  val lightColor = ContextCompat.getColor(context, R.color.cardview_light_background)
  @SuppressLint("PrivateResource") //
  val darkColor = ContextCompat.getColor(context, R.color.cardview_dark_background)

  private var _playerView: PlayerView? = null
  private var _textView: TextView? = null

  override var playerView: PlayerView?
    get() = this._playerView
    @SuppressLint("SetTextI18n")
    set(value) {
      if (value != this._playerView) {
        if (this._playerView != null && this._textView != null) {
          this._playerView?.removeView(this._textView)
          this._textView = null
        }
        this._playerView = value
        this._playerView?.let {
          it.removeAllViews()
          this._textView =
            from(it.context).inflate(R.layout.dummy_textview, it, false) as TextView
          it.addView(_textView)
          _textView?.text = "Chilling \\m/"
        }
      }
    }

  override var playbackInfo: PlaybackInfo
    get() = PlaybackInfo.SCRAP
    set(value) {}

  override var parameters: PlaybackParameters
    get() = PlaybackParameters.DEFAULT
    set(value) {}

  override var repeatMode: Int
    get() = Playable.REPEAT_MODE_ONE
    set(value) {}

  override val isPlaying: Boolean
    get() = false

  override val volumeInfo: VolumeInfo
    get() = VolumeInfo.SCRAP

  @SuppressLint("SetTextI18n")
  override fun prepare(loadSource: Boolean) {
    _textView?.apply {
      text = "Prepared"
      setTextColor(darkColor)
    }
    _playerView?.setBackgroundColor(lightColor)
  }

  @SuppressLint("SetTextI18n")
  override fun play() {
    _textView?.apply {
      text = "Playing"
      setTextColor(lightColor)
    }
    _playerView?.setBackgroundColor(darkColor)
  }

  @SuppressLint("SetTextI18n")
  override fun pause() {
    _textView?.apply {
      text = "Paused"
      setTextColor(darkColor)
    }
    _playerView?.setBackgroundColor(lightColor)
  }

  @SuppressLint("SetTextI18n")
  override fun reset() {
    _textView?.apply {
      text = "Reset"
      setTextColor(darkColor)
    }
    _playerView?.setBackgroundColor(lightColor)
  }

  @SuppressLint("SetTextI18n")
  override fun release() {
    _textView?.apply {
      text = "Released"
      setTextColor(darkColor)
    }
    _playerView?.setBackgroundColor(lightColor)
    this.playerView = null
  }

  override fun addEventListener(listener: PlayerEventListener) {

  }

  override fun removeEventListener(listener: PlayerEventListener?) {

  }

  override fun addVolumeChangeListener(listener: VolumeChangedListener) {

  }

  override fun removeVolumeChangeListener(listener: VolumeChangedListener?) {

  }

  override fun addErrorListener(errorListener: ErrorListener) {

  }

  override fun removeErrorListener(errorListener: ErrorListener?) {

  }

  override fun setVolumeInfo(volumeInfo: VolumeInfo): Boolean {
    return true
  }
}