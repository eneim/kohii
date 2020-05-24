# Change Log

## 1.1.0.2011003

_Under development_

- ExoPlayer: 2.11.3
- Support _multiple playback_. Check out [docs](https://eneim.github.io/kohii/usage/advance/) for more information.
- Experimental manual playback.
- Introduce `PlayableObserver`.
- Introduce `activeLifecycleState` setting for registering new manager.
- Introduce `PlayerParameters`, `NetworkTypeChangeListener`.
- Deprecate the `VideoSize`.
- Introduce the `initialPlaybackInfo` setting for the Binder.
- Add method `Kohii.createControlDispatcher(Playback)`.
- Add method `Playback.Controller.setupRenderer(Playback, Any?)`.
- Add method `Playback.Controller.teardownRenderer(Playback, Any?)`.

- [Breaking] Rename `Playable#considerRequestRenderer` -> `Playable#setupRenderer`.
- [Breaking] Rename `Playable#considerReleaseRenderer` -> `Playable#teardownRenderer`.
- [Breaking] `RendererProvider#releaseRenderer` now needs to return a boolean.
- [Breaking] `Playback#addCallback` and `Playback#removeCallback` are now internal.
- [Breaking] The `DefaultControlDispatcher` is now internal.

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
