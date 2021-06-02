# Change Log

## [1.1.1.2011003]

### Added

- ExoPlayer: 2.11.7
- Fix an issue that cause cache doesn't write to file (https://github.com/eneim/kohii/pull/91);

## 1.1.0.2011003

- ExoPlayer: 2.11.3
- Support _multiple playback_. Check out [docs](https://eneim.github.io/kohii/usage/advance/) and [recipe](https://www.notion.so/00ea153b5378454dbc7a104733cd01d9) for more information.
- Experimental manual playback. Read more [here](https://www.notion.so/Manual-playback-controller-in-Kohii-212927dd75de4971bc28d27e4b34911a).
- Add method `Kohii.createControlDispatcher(Playback)`. Read more [here](https://www.notion.so/Manual-playback-controller-in-Kohii-212927dd75de4971bc28d27e4b34911a#999cb7749ed74cf9aa1c2329c093401f).
- Introduce `PlayableObserver`, Read more [here](https://www.notion.so/8ebea74d5e3347c580209652f374247c).
- Introduce `activeLifecycleState` setting for the Manager. Default to `State.STARTED`. All the playbacks in a Manager can be played only if the lifecycle state of the Manager is at least this value.
- Experimental `PlayerParameters`, `NetworkTypeChangeListener`: the callback from a `NetworkTypeChangeListener` needs to return a `PlayerParameters` value so that the player can switch the video/audio quality, resolution, etc.  
- Deprecate the `VideoSize`. Its value is no longer used anymore in the library.
- Experimental `initialPlaybackInfo` setting for the Binder. Setting this value will allow the Playback to start from a specific `PlaybackInfo` value.
- Experimental `releaseOnInActive` setting for the Binder. Setting this to false will let the Playback keep its state when it is inactive, but not yet detached from the Manager.
- Add method `Playback.Controller.setupRenderer(Playback, Any?)` and `Playback.Controller.teardownRenderer(Playback, Any?)`. Client can configure the renderer with custom manual control logic.
- Support Playback locking: if a Playback is locked, it will still be selected but will not be played.
- Add `ExoPlayerCache` to the `kohii-exoplayer` package. It can be used to obtain a pre-built Cache easily.
- Add `TrackSelectorFactory`, `LoadControlFactory` to the `kohii-exoplayer` package.
- Add `ExoPlayerConfig` to gather most of the detailed setting for a `SimpleExoPlayer` instance.
- Add `createKohii` convenient methods to easily create `Kohii` instance with custom parameters.
- Add `Engine.lock*` and `Engine.unlock*` methods to support manual lock/unlock an Activity/Manager/Bucket or Playback.
- Add a simple demonstration that builds TikTok-alike UI/UX: [source code](https://github.com/eneim/kohii/tree/dev-v1/kohii-sample-tiktok).
- Another document hub for Kohii: https://www.notion.so/The-Kohii-Library-c89a75e2df2b485391b425b5dc83adce

- **[Breaking]** Rename `Playable#considerRequestRenderer` -> `Playable#setupRenderer`.
- **[Breaking]** Rename `Playable#considerReleaseRenderer` -> `Playable#teardownRenderer`.
- **[Breaking]** `RendererProvider#releaseRenderer` now needs to return a boolean.
- **[Breaking]** `Playback#addCallback` and `Playback#removeCallback` are now internal.
- **[Breaking]** The `DefaultControlDispatcher` is now internal.
- **[Breaking]** Include Playback in the `ArtworkHintListener#onArtworkHint`.
- **[Breaking]** Remove default implementations for `BandwidthMeterFactory`.

## 1.0.0.2010004

_2020.03.15_

- Open `Builder` for `Kohii` and `Latte`. Clients can now use custom implementations to construct
those classes. Example usages:

```Kotlin
val kohii = Kohii.Builder(context)
    .setPlayableCreator(customPlayableCreator)
    .build()
```

## 1.0.0.2010004-beta.5

Experiment release for `Bucket.Selector`, usage is not finalized yet. Currently it is proved to allow select multiple Playbacks for a Bucket, but finally only one Playback will be picked to play by the Group.

- Add `@JvmOverloads` annotation to support calling from Java.
- Other internal improvements.

## 1.0.0.2010004-beta.3

_2019.12.18_

> This is the very first release that is documented. Please visit [document page](https://eneim.github.io/kohii) for detail.

- First public beta release.
