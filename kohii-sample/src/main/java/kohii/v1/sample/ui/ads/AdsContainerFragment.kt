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

package kohii.v1.sample.ui.ads

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.ads.AdMediaItem
import kohii.v1.ads.Manilo
import kohii.v1.core.Common
import kohii.v1.core.controller
import kohii.v1.sample.DemoApp
import kohii.v1.sample.common.ViewBindingFragment
import kohii.v1.sample.databinding.FragmentAdsListBinding
import okio.buffer
import okio.source
import timber.log.Timber

class AdsContainerFragment :
    ViewBindingFragment<FragmentAdsListBinding>(FragmentAdsListBinding::inflate) {

  private companion object {
    const val STATE_AD_SAMPLE = "dev_ad_sample"
  }

  private lateinit var manilo: Manilo
  private lateinit var adSamples: AdSamples

  private var selectedAdSample: AdSample? = null
    @SuppressLint("SetTextI18n")
    set(value) {
      val current = field
      field = value
      if (value == null) {
        if (current != null) {
          manilo.cancel("$current")
        }
        requireBinding().adInfo.text = "No sample selected."
      } else {
        if (value != current) {
          val adMedia = AdMediaItem(
              value.contentUri,
              value.adTagUri
          )

          manilo.setUp(adMedia) {
            tag = "$value"
            repeatMode = Common.REPEAT_MODE_ONE
            controller = controller { _, renderer ->
              if (renderer is PlayerView) {
                renderer.useController = true
              }
            }
          }.bind(requireBinding().playerView)
          requireBinding().adInfo.text = value.name
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val app = (requireContext().applicationContext as DemoApp)
    manilo = app.manilo
    adSamples = app.moshi
        .adapter(AdSamples::class.java)
        .fromJson(
            app.assets.open("ads.json").source().buffer()
        ) ?: AdSamples("No Ads", emptyList())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    manilo.register(this).addBucket(requireBinding().playerContainer)

    manilo.addAdEventListener {
      Timber.d("AdEventListener received : $it")
    }

    val layoutManager = LinearLayoutManager(view.context)
    requireBinding().adsContainer.layoutManager = layoutManager
    requireBinding().adsContainer.adapter = object : Adapter<ViewHolder>() {
      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_activated_1, parent, false)
        val viewHolder = object : ViewHolder(itemView) {}
        itemView.setOnClickListener {
          if (viewHolder.adapterPosition in 0 until itemCount) {
            selectedAdSample = adSamples.samples[viewHolder.adapterPosition]
            // Update the UI state of all visible items. Not an optimized practice.
            notifyItemRangeChanged(
                layoutManager.findFirstVisibleItemPosition(),
                layoutManager.findLastVisibleItemPosition()
            )
          }
        }
        return viewHolder
      }

      override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val adSample = adSamples.samples[position]
        (holder.itemView as TextView).text = adSample.name
        (holder.itemView as TextView).isActivated = adSample == selectedAdSample
      }

      override fun getItemCount(): Int = adSamples.samples.size
    }

    selectedAdSample = savedInstanceState?.getParcelable(STATE_AD_SAMPLE)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    selectedAdSample?.let { sample ->
      outState.putParcelable(STATE_AD_SAMPLE, sample)
    }
  }
}
