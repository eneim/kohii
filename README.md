# Kohii

## kohii (コーヒー、[[ko̞ːçiː]](https://en.wiktionary.org/wiki/%E3%82%B3%E3%83%BC%E3%83%92%E3%83%BC))

<img src="art/kohii.png?raw=true" alt="Kohii" width="384">

Video playback for Android made easy.

![Bintray](https://img.shields.io/bintray/v/eneimlabs/kohii/kohii)
![Maven Central](https://img.shields.io/maven-central/v/im.ene.kohii/kohii)
[![](https://yourdonation.rocks/images/badge.svg)](https://github.com/sponsors/eneim)

**Kohii** is a high level Video playback library, built from the experience creating [toro](https://github.com/eneim/toro) and contributing to [ExoPlayer](https://github.com/google/ExoPlayer). **Kohii** focuses on the Video playback on Android, giving developer powerful playback control, including 

1. Easy way to start a Video playback with confidence (hint: only one line), 
2. Smooth playback experience on ~~list~~ any Views (RecyclerView, NestedScrollView, ViewPager2, etc).
3. Smooth configuration change handling, including the transition from local playback to fullscreen playback and vice versa. 

## Demo

|Automatic playback|Playback continuity (Seamless fullscreen)|
| :--- | :--- |
|<img src="./art/kohii_demo_2.gif" width="216" height="468"/>|<img src="./art/kohii_demo_3.gif" width="468" height="468"/>|

## Setup

Add to your module's build.gradle dependencies

```groovy
// Update top level build.gradle
allprojects {
  repositories {
    jcenter()
  }
}
```

```groovy
// Add these to app level build.gradle (or to module that will use Kohii)
implementation "im.ene.kohii:kohii:1.0.0.2010004-B1" // core library
implementation "im.ene.kohii:kohii-exoplayer:1.0.0.2010004-B1" // default support for ExoPlayer
implementation "com.google.android.exoplayer:exoplayer:2.10.4" // required ExoPlayer implementation.
```

## Start a playback

```Kotlin tab=
// TODO: Have a videoUrl first.
// 1. Initialization in Fragment or Activity
val kohii = Kohii[this@Fragment]
kohii.register(this /* Fragment or Activity */).addBucket(this.recyclerView)

// 2. In ViewHolder or Adapter: bind the video to the PlayerView inside a child of the RecyclerView.
kohii.setUp(videoUrl).bind(playerView)
```

```Java tab=
// TODO: Have a videoUrl first.
// 1. Initialization in Fragment or Activity
Kohii kohii = Kohii.get(this);
kohii.register(this).addBucket(this.recyclerView);

// 2. In ViewHolder or Adapter: bind the video to the PlayerView inside a child of the RecyclerView.
kohii.setUp(videoUrl).bind(playerView);
```

## Requirements

**Kohii** works on Android 4.4+ (API level 19+) and on Java 8+. It is recommended to use Kohii using Kotlin.

The core library doesn't come with any actual playback logic. The implementation requires corresponding 3rd libraries: ``kohii-exoplayer`` will requires ``exoplayer``, ``kohii-androidx`` will requires ``androidx.media2``.