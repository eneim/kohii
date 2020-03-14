# Advance usages

Advance usages session will focus on using **Kohii** with **ExoPlayer**. Using others _extensions_ will be discussed in more advance topics, because each extension is built with different ability.

Here, we assume that you are familiar with **ExoPlayer**, including its core components (Player, ExoPlayer, SimpleExoPlayer, MediaSource, etc) and UI system (PlayerView, PlayerControlView, ControlDispatcher, etc).

This session may use some keywords about components in **Kohii** like **Container**, **Bucket**, etc. You can find their definitions here: [Components](../../customize/terms).

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

If you still want to use the default `PlayerViewPlayableCreator`, it can be constructed by its own Builder too, which will requires a `BridgeCreatorFactory` which is of type `Context.() -> BridgeCreator<PlayerView>`:

```Kotlin tab=
val playableCreator: PlayableCreator<PlayerView> =
  PlayerViewPlayableCreator.Builder(this).setBridgeCreatorFactory(factory).build()
```

Please take a look at the source for all available builder parameters.

## Using unique tag

To have the automatic playback works, you just need this:

```Kotlin tab=
kohii.setUp(videoUrl).bind(playerView)
```

```Java tab=
kohii.setUp(videoUrl).bind(playerView);
```

With this one line, once user scrolls some Videos off screen, and then scrolls them back, their previous playback position will be reset and Videos will start from the beginning. If you want those Videos to start from where it was before being scrolled off screen, you need to set a **_application wide unique tag_** for the Video. Sample code as below:

```Kotlin tab=
kohii.setUp(videoUrl) {
  tag = videoUniqueTag
}
    .bind(playerView)
```

By having a unique tag, your Video state will also be retained across configuration changes like Window size change, orientation change or other system config changes. So it is highly recommended to have unique tags for your Videos.

| With tag                                            | Without tag                                            |
| :-------------------------------------------------- | :----------------------------------------------------- |
| <img src="/assets/kohii_with_tag.gif" width="288"/> | <img src="/assets/kohii_without_tag.gif" width="288"/> |

## Show/Hide thumbnail

**Kohii** allows client to implement a special interface called [`ArtworkHintListener`](../../api/kohii-core/kohii.v1.core/-playback/-artwork-hint-listener/). With this interface, **Kohii** can tell client when it should show/hide thumbnail (or _artwork_ in **Kohii**'s term). A sample code is as below:

```Kotlin
// 1. Let ViewHolder implement ArtworkHintListener interface.
class VideoViewHolder(itemView: View): ViewHolder(itemView), ArtworkHintListener {

  val thumbnail: ImageView = // ... this is the ImageView for thumbnail.

  // Using this callback to show/hide thumbnail.
  override fun onArtworkHint(
    shouldShow: Boolean,
    position: Long,
    state: Int
  ) {
    thumbnail.isVisible = shouldShow
  }
}

// 2. Provide the ArtworkHintListener when setting up the Video in ViewHolder
kohii.setUp(assetVideoUri) {
  artworkHintListener = this@VideoViewHolder
}
  .bind(playerView)
```

You can see this setup in action in [this demo](https://github.com/eneim/kohii/tree/dev-v1/kohii-sample/src/main/java/kohii/v1/sample/ui/grid).

In the callback `onArtworkHint`, other than a boolean flag `shouldShow` which is a hint from system about if the artwork should be shown or hidden, there is also `position` which is the current position of the Video, and `state` which is current state of the player. Using these values, you can have your own behavior, other than just show/hide the ImageView.

## Switching a Video playback between renderers

In practice, you may find yourself try to _bring_ a Video from this `PlayerView` to another `PlayerView`. Doing so can be as simple as calling bind to the destination `PlayerView`:

```Kotlin
kohii.setUp(videoUrl) {
  tag = videoTag
}.bind(olayerView1)

// Later, switch to another `PlayerView`
kohii.setUp(videoUrl) {
  tag = videoTag
}.bind(playerView2)
```

!!! note
    Note that you need to set unique `tag` to the Video, so that after switching to another `PlayerView`, it keeps playing smoothly, without being reset to beginning.

To help you simplify the steps, the call `bind(playerView1)` with a valid tag will return an object called [`Rebinder`](../../api/kohii-core/kohii.v1.core/-rebinder/). This `Rebinder` has one method `bind` so you can reuse this object to easily rebind a Video to any `PlayerView`. `Rebinder` is also a `Parcelable`, so you can pass this object around.

Please check [this demo](https://github.com/eneim/kohii/tree/dev-v1/kohii-sample/src/main/java/kohii/v1/sample/ui/sview) to see how it uses `Rebinder` to switch a Video from `PlayerView` to dialog and back.

## Reuse PlayerView instance for multiple Videos

Until now, the setup code is always `kohii.setUp(videoUrl).bind(playerView)` which may let you think that you will need to bind the Video to a `PlayerView` instance.

In **Kohii**, target of the method `bind` is called [`container`](../../customize/terms/#renderer-and-container). While `PlayerView` is the final place where Video content is rendered (and therefore it is called [`renderer`](../../customize/terms/#renderer-and-container), it can also be a `container` which will _contain itself_).

You can bind the Video to any `ViewGroup` as container, as long as either it is a `renderer` itself, or it has no children so that it _can contain_ a `renderer` later. When you bind to a _non-renderer_ `container`, for example an empty `FrameLayout`, **Kohii** will automatically prepare and add the `PlayerView` instance to that `FrameLayout` dynamically. At the same time, the unnecessary `PlayerView` instance will be removed from `container` and put back to a `Pool`. This way, only a few `PlayerView` instances will be created and reused for as many container/Videos as possible.

By default, **Kohii** has its own logic for creating and recycling `PlayerView`, but developers can build their own by extending [**Engine**](../../api/kohii-core/kohii.v1.core/-engine) - another important component of **Kohii**. Extending **Engine** and building custom playback logic will be discussed in topics for developers.

## Using MemoryMode to improve UX

Your screen may contain many Videos at a time, and preload Videos forward so they can start as soon as possible is a legit requirement. In practice, preload many Videos consumes a lot of system resource like memory and network. To address this need in proper way, **Kohii** provide a special control flag called [MemoryMode](../../api/kohii-core/kohii.v1.core/-memory-mode/). The idea behinds `MemoryMode` is as below:

- The idea is to allow **Kohii** to _load around Video of interest[^1]_. When the Video of interest is playing, at the same time **Kohii** will prepare the closet Videos _around_ it: the first on top, the first below, the first to the left and/or the first to the right, and the second closet Videos ...

- To control the maxinum number of Videos to prepare, **Kohiii** needs client to explixitly define this (with a default behavior to be automatic).

`MemoryMode` is the information client uses to define this behavior. To use it, you need to update the code as below:

```diff
- kohii.register(this)
+ kohii.register(this, MemoryMode.BALANCED)
```

While allowing client to define this behavior, **Kohii** will strictly observe system memory status, and will override that behavior on demand to prevent your app from behaving unexpectedly.

## Manual controller (experiment)

_COMING SOON_

[^1]: The Video that is selected to play
