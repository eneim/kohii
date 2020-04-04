## Switching a Video playback between renderers

In practice, you may find yourself try to _bring_ a Video from a `PlayerView` to another `PlayerView`. Doing so can be as simple as calling bind to the destination `PlayerView`:

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
    Note that you need to set the same unique `tag` to the Video, so that after switching to another `PlayerView`, it keeps playing smoothly, without being reset to beginning.

To help you simplify the steps, the call `bind(playerView1)` with a valid tag will return an object called [`Rebinder`](../../api/kohii-core/kohii.v1.core/-rebinder/). This `Rebinder` has one method `bind` so you can reuse this object to easily rebind a Video to any `PlayerView`. `Rebinder` is also a `Parcelable`, so you can pass this object around.

Please check [this demo](https://github.com/eneim/kohii/tree/dev-v1/kohii-sample/src/main/java/kohii/v1/sample/ui/sview) to see how it uses `Rebinder` to switch a Video from `PlayerView` to dialog and back.
