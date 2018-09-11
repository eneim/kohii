# kohii (コーヒー、[[ko̞ːçiː]](https://en.wiktionary.org/wiki/%E3%82%B3%E3%83%BC%E3%83%92%E3%83%BC))

<img src="art/kohii.png?raw=true" alt="Kohii" width="384">

Video playback for Android made easy.

![](https://img.shields.io/nexus/r/https/oss.sonatype.org/im.ene.kohii/kohii.svg)
![](https://img.shields.io/nexus/s/https/oss.sonatype.org/im.ene.kohii/kohii.svg)

> Kohii is in heavy development and not ready for production use. Also it will not deprecate **Toro** any time soon.

Kohii is a high level Video playback library, built from the experience creating/maintaining [toro](https://github.com/eneim/toro) and [ExoPlayer](https://github.com/google/ExoPlayer). Kohii targets directly the Video playback on Android, giving developer powerful playback control, including **(1)** easy way to start a Video playback with confidence (hint: only one line), **(2)** smooth transition from local playback to fullscreen playback and vice versa, **(3)** smooth playback experience on list (RecyclerView, NestedScrollView, etc).

## Setup

Add to your module's build.gradle dependencies

```groovy
implementation "im.ene.kohii:kohii:1.0.0.2802-ALPHA1"
implementation "com.google.android.exoplayer:exoplayer:2.8.2"
```

## Start a playback

```kotlin
// have a videoUrl first.
Kohii[fragment].setUp(videoUrl).asPlayable().bind(playerView)
```
