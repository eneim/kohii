## Using Builder

**Kohii** instance can be constructed using `Builder`. By default, calling `Kohii[context]` will create or reuse an instance with default implementation. For advance users, it is more flexible to be able to customize this. **Kohii** provides `Builder` to make this happen:

```Kotlin tab=
val playableCreator = MyCustomPlayableCreator()
val builder = Kohii.Builder(context)
    .setPlayableCreator(playableCreator)
val kohii = builder.build()
```

```Java tab=
PlayableCreator<PlayerView> playableCreator = new MyCustomPlayableCreator();
Kohii.Builder builder = new Kohii.Builder(context)
    .setPlayableCreator(playableCreator);
Kohii kohii = builder.build();
```

If you still want to use the default `PlayerViewPlayableCreator`, it can be constructed by its own Builder too, which will requires a `PlayerViewBridgeCreatorFactory` which is of type `(Context) -> BridgeCreator<PlayerView>`:

```Kotlin tab=
val playableCreator: PlayableCreator<PlayerView> = 
        PlayerViewPlayableCreator.Builder(this)
                .setBridgeCreatorFactory(myFactory).build()
```

A full Kohii example:

```kotlin
val kohii = Kohii.Builder(context)
        .setPlayableCreator(
            PlayerViewPlayableCreator.Builder(context)
                .setBridgeCreatorFactory {
                  PlayerViewBridgeCreator(myPlayerProvider, myMediaSourceFactoryProvider)
                }
                .build()
        )
        .setRendererProviderFactory(myFactory)
        .build()
```

Please take a look at the source for all available builder parameters.

## Using extension methods

_From v1.1.0.2011003_

You also have more advance ways to construct new **Kohii** instance:

```kotlin
val kohii = createKohii(
    context = context,
    config = ExoPlayerConfig.DEFAULT
)
```

Where [ExoPlayerConfig](../api/kohii-exoplayer/kohii.v1.exoplayer/-exo-player-config/) is the combination of many base parameters to construct ExoPlayer's components like the `LoadControl`, `DefaultTrackSelector`, `DefaultBandwidthMeter`, etc. If you have existing parameter to reuse, you can use this convenient to build a **Kohii** instance using them. `ExoPlayerConfig.DEFAULT` is the default configuration where the parameters are the same as default ExoPlayer's setup.

If you want to reuse the already-built ExoPlayer components (`LoadControl`, `DefaultTrackSelector`, `DefaultBandwidthMeter`, etc) instead, you can also use the second convenient creator below:

```kotlin
val kohii = createKohii(
    context = context,
    playerCreator = myPlayerCreator,
    mediaSourceFactoryCreator = myMediaSourceFactoryCreator,
    rendererProviderFactory = myFactory
)
```

Using this method, you can pass your custom way of creating a new [Player](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/Player.html) instance,
[MediaSourceFactory](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/source/MediaSourceFactory.html) instance and [RendererProvider](../api/kohii-core/kohii.v1.core/-renderer-provider/) instance. Each parameter comes with a default value.
