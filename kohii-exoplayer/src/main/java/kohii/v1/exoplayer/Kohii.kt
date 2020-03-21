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

package kohii.v1.exoplayer

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.core.Engine
import kohii.v1.core.Manager
import kohii.v1.core.Master
import kohii.v1.core.PlayableCreator
import kohii.v1.core.RendererProviderFactory
import kohii.v1.utils.SingletonHolder
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class Kohii private constructor(
  master: Master,
  playableCreator: PlayableCreator<PlayerView> = PlayerViewPlayableCreator(master),
  private val rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }
) : Engine<PlayerView>(master, playableCreator) {

  private constructor(context: Context) : this(Master[context])

  companion object : SingletonHolder<Kohii, Context>(::Kohii) {

    @JvmStatic // convenient static call for Java
    operator fun get(context: Context) = super.getInstance(context)

    @JvmStatic // convenient static call for Java
    operator fun get(fragment: Fragment) = get(fragment.requireContext())
  }

  init {
    // Adapt from ExoPlayer demo app.
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    if (CookieHandler.getDefault() !== cookieManager) {
      CookieHandler.setDefault(cookieManager)
    }
  }

  override fun prepare(manager: Manager) {
    manager.registerRendererProvider(PlayerView::class.java, rendererProviderFactory())
  }

  class Builder(context: Context) {

    private val master = Master[context.applicationContext]

    private var playableCreator: PlayableCreator<PlayerView> =
      PlayerViewPlayableCreator(master)

    private var rendererProviderFactory: RendererProviderFactory = { PlayerViewProvider() }

    fun setPlayableCreator(playableCreator: PlayableCreator<PlayerView>): Builder = apply {
      this.playableCreator = playableCreator
    }

    fun setRendererProviderFactory(factory: RendererProviderFactory): Builder = apply {
      this.rendererProviderFactory = factory
    }

    fun build(): Kohii = Kohii(
        master = master,
        playableCreator = playableCreator,
        rendererProviderFactory = rendererProviderFactory
    ).also {
      master.registerEngine(it)
    }
  }
}
