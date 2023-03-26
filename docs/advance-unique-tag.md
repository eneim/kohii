## Using unique tag

To have the automatic playback works, you just need this:

=== "Kotlin"
    ```Kotlin
    kohii.setUp(videoUrl).bind(playerView)
    ```

=== "Java"
    ```Java
    kohii.setUp(videoUrl).bind(playerView);
    ```

With this one line, once user scrolls some Videos off screen, and then scrolls them back, the
playback position will be reset and the Videos will start from the beginning. If you want the Videos
to start from where it was before being scrolled off screen, you need to set an **_application wide
unique tag_** for the Video. Sample code as below:

=== "Kotlin"
```Kotlin
kohii.setUp(videoUrl) {
tag = videoUniqueTag
}
.bind(playerView)
```

By having a unique tag, your Video state will also be retained across configuration changes like
Window size change, orientation change or other system config changes. So it is highly recommended
to have unique tags for your Videos.

| With tag                                            | Without tag                                            |
| :-------------------------------------------------- | :----------------------------------------------------- |
| <img src="../../../assets/kohii_with_tag.gif" width="288"/> | <img src="../../../assets/kohii_without_tag.gif" width="288"/> |
