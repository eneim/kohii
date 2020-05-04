# Change Log

## 1.1.0.2011003

_Under development_

- Support _multiple playback_. Check out [docs](https://eneim.github.io/kohii/usage/advance/) for more information.
- Experimental manual playback.
- Introduce `PlayableObserver`.
- Introduce `activeLifecycleState` setting for registering new manager.
- Introduce `PlayerParameters`, `NetworkTypeChangeListener`.
- Deprecate the `VideoSize`.
- Introduce the `initialPlaybackInfo` setting for the Binder. 

## 1.0.0.2010004

_2020.03.015_

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
