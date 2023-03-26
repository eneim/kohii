## Show/Hide thumbnail

**Kohii** provides a special interface
called [`ArtworkHintListener`](../api/kohii-core/kohii.v1.core/-playback/-artwork-hint-listener/).
With this interface, **Kohii** can tell client when it should show/hide thumbnail (or _artwork_
in **Kohii**'s term). Sample code:

```Kotlin
// 1. Let ViewHolder implement ArtworkHintListener interface.
class VideoViewHolder(itemView: View): ViewHolder(itemView), ArtworkHintListener {

  val thumbnail: ImageView = // ... this is the ImageView for the thumbnail.

  // Override this callback to show/hide thumbnail.
  override fun onArtworkHint(
    playback: Playback,
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

You can see this setup in action
in [this demo](https://github.com/eneim/kohii/tree/dev-v1/kohii-sample/src/main/java/kohii/v1/sample/ui/grid)
.

In the callback `onArtworkHint`, other than a boolean flag `shouldShow` which is a hint from **
Kohii** about if the artwork should be shown or hidden, there is also `position` which is the
current position of the Video, and `state` which is current state of the player. Using these values,
you can have your own behavior, other than just show/hide the ImageView.
