# Kohii

## kohii (コーヒー、[[ko̞ːçiː]](https://en.wiktionary.org/wiki/%E3%82%B3%E3%83%BC%E3%83%92%E3%83%BC))

<img src="art/kohii.png?raw=true" alt="Kohii" width="384">

Video playback for Android made easy.

![Bintray](https://img.shields.io/bintray/v/eneimlabs/kohii/kohii-core)
![Maven Central](https://img.shields.io/maven-central/v/im.ene.kohii/kohii-core)
[![](https://yourdonation.rocks/images/badge.svg)](https://github.com/sponsors/eneim)

**Kohii** is a powerful, easy to use and extensible Video playback library. **Kohii** provides powerful features out of the box, including 

1. Easy to start: just one line to start a Video playback.
2. Automatic playback experience on RecyclerView, NestedScrollView, ViewPager2, etc.
3. Configuration change handling works out of the box, including the transition from local playback to fullscreen playback and vice versa. 
4. Feature-rich sample app that covers either simple or advance use cases.
5. Extension-based structure, including default implementations that support ExoPlayer, AndroidX Media2, YouTube Player SDK. 

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
    // mavenCentral() should also work.
  }
}
```

```groovy
// Add these to app level build.gradle (or to module that will use Kohii)
implementation "im.ene.kohii:kohii-core:1.0.0.2010004" // core library
implementation "im.ene.kohii:kohii-exoplayer:1.0.0.2010004" // default support for ExoPlayer
implementation "com.google.android.exoplayer:exoplayer:2.10.4" // required ExoPlayer implementation.
```

You will need to set this flag to your build.gradle too: `-Xjvm-default=enable`.

```groovy
android {
  kotlinOptions {
    freeCompilerArgs += [
        '-Xjvm-default=enable'
    ]
  }
}
```

## Start a playback

```Kotlin tab=
// Kotlin
// 1. Initialization in Fragment or Activity
val kohii = Kohii[this@Fragment]
kohii.register(this /* Fragment or Activity */).addBucket(this.recyclerView)

// 2. In ViewHolder or Adapter: bind the video to the PlayerView inside a child of the RecyclerView.
kohii.setUp(videoUrl).bind(playerView)
```

```Java tab=
// Java
// 1. Initialization in Fragment or Activity
Kohii kohii = Kohii.get(this);
kohii.register(this).addBucket(this.recyclerView);

// 2. In ViewHolder or Adapter: bind the video to the PlayerView inside a child of the RecyclerView.
kohii.setUp(videoUrl).bind(playerView);
```

## Requirements

**Kohii** works on Android 4.4+ (API level 19+) and on Java 8+. It is recommended to use Kohii using Kotlin.

The core library doesn't come with any actual playback logic. Actual implementation comes with extension libraries. The extensions require corresponding 3rd libraries: ``kohii-exoplayer`` will requires ``exoplayer``, ``kohii-androidx`` will requires ``androidx.media2``.
