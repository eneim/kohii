# Kohii

## kohii (コーヒー、[[ko̞ːçiː]](https://en.wiktionary.org/wiki/%E3%82%B3%E3%83%BC%E3%83%92%E3%83%BC))

<img src="art/kohii.png?raw=true" alt="Kohii" width="384">

Video playback for Android made easy.

![Bintray](https://img.shields.io/bintray/v/eneimlabs/kohii/kohii-core)
![Maven Central](https://img.shields.io/maven-central/v/im.ene.kohii/kohii-core)
[![Support](https://yourdonation.rocks/images/badge.svg)](https://github.com/sponsors/eneim)

**Kohii** is a powerful, extensible, yet easy to use and extensible Video playback library. **Kohii** provides useful features out of the box, including

- [x] Easy to start: just one line to start a Video playback.
- [x] Automatic playback experience on RecyclerView, NestedScrollView, ViewPager2, etc.
- [x] Configuration change handling works out of the box, including the switching from in-list player to fullscreen player and vice versa.
- [x] Feature-rich sample app that covers either simple or advance use cases.
- [x] Extensible, including default implementations that support ExoPlayer, AndroidX Media2, YouTube Player SDK.

## Demo

| Automatic playback| Playback continuity (Seamless fullscreen)|
| :- |:- |
| <img src="./art/kohii_demo_2.gif" width="216"/> | <img src="./art/kohii_demo_3.gif" width="468"/> |

## Setup

Add to your module's build.gradle dependencies

```groovy
// Update top level build.gradle
allprojects {
  repositories {
    jcenter() // mavenCentral() should also work.
  }
}
```

```groovy
// Add these to app level build.gradle (or to module that will use Kohii)
def kohiiVersion = '1.0.0.2010004'
def exoPlayerVersion = '2.10.4'

implementation "im.ene.kohii:kohii-core:${kohiiVersion}"
implementation "im.ene.kohii:kohii-exoplayer:${kohiiVersion}"
implementation "com.google.android.exoplayer:exoplayer:${exoPlayerVersion}"
```

## Start a playback

Assuming that you have a `Fragment` which contains a `RecyclerView`, and you want to play a Video using a `PlayerView` placed inside a `ViewHolder` of the `RecyclerView`, below is what you need to do using `Kohii`:

```Kotlin tab=
// Kotlin
// TODO: Have a videoUrl first.
// 1. Initialization in Fragment
val kohii = Kohii[this@Fragment]
kohii.register(this@Fragment).addBucket(this.recyclerView)

// 2. In ViewHolder or Adapter: bind the video to the PlayerView.
kohii.setUp(videoUrl).bind(playerView)
```

```Java tab=
// Java
// TODO: Have a videoUrl first.
// 1. Initialization in Fragment
Kohii kohii = Kohii.get(this);
kohii.register(this).addBucket(this.recyclerView);

// 2. In ViewHolder or Adapter: bind the video to the PlayerView.
kohii.setUp(videoUrl).bind(playerView);
```

## Requirements

**Kohii** works on Android 4.4+ (API level 19+) and on Java 8+. It is recommended to use **Kohii** using the **Kotlin&trade;** language.

The core library doesn't come with any actual playback logic. Actual implementation comes with extension libraries. The extensions require corresponding 3rd libraries: `kohii-exoplayer` will require `exoplayer`, `kohii-androidx` will require `androidx.media2`.
