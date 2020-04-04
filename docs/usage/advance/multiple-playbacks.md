## Playing many Videos at the same time

_Available from v1.1.0.2011003_

<img src="../../../assets/kohii_demo_multi_player.gif" width="216" style="display: block; margin: 0 auto;"/>

From v1.1.0.2011003, **Kohii** adds _Playback Selector_ and _Playback Strategy_ to support multiple playbacks. The _Selector_ is a _Single Abstract Method_ that accepts a collection of _candidate_ (= the Playbacks that can play the media) and returns a collection of Playback that should play the media.

This feature is enabled at [Bucket](/customize/terms/#bucket-manager-and-group) level. Which means that: client can have multiple playbacks in a **Bucket** by using correct _Strategy_ and _Selector_. The setup is easy: you can set the _Strategy_ and _Selector_ at the time you add the **Bucket**.

```Kotlin tab=
kohii.register(this)
    .addBucket(
        view = recyclerView,
        strategy = Strategy.MULTI_PLAYER,
        selector = { candidates ->
          candidates.take(2)
        }
    )
```

The code above will: add a new **Bucket** for the `recyclerView`, with `MULTI_PLAYER` Strategy and using a Selector that will select up to 2 Playbacks from the candidates to play.

Note that, _Strategy_ and _Selector_ need to be set together to enable the multiple playbacks. If the client uses a _Selector_ that selects many Playbacks, but uses the `SINGLE_PLAYER` Strategy, it will only play one Playback.

The available _Strategies_ are:

- `MULTI_PLAYER`: play all Playbacks selected by the _Selector_.
- `SINGLE_PLAYER`: play the first available Playback from the list selected by the _Selector_.
- `NO_PLAYER`: do not let the _Selector_ select anything.

**NOTE**: Mutiple playbacks comes with a caveat. In Video playback, audio focus is an important aspect. The client needs to not only respect the audio focus given by system, but also to respect the audio focuses among a Video with the others in the same Application. Therefore, when the client enable `MULTI_PLAYER` _Strategy_, the library will forcefully mute the audio of all available Playbacks, regardless the number of Playbacks selected by the _Selector_. Changing to `SINGLE_PLAYER` or `NO_PLAYER` _Strategy_ will switch everything back to normal.
