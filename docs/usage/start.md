# Getting start

**Kohii** is an Android library, therefore to use it, you need to add its artifacts to your module's `build.gradle` dependency list. **Kohii** is released to both `bintray jcenter repository` and `sonatype maven central repository`, so before adding **Kohii** to your project, make sure the following lines exist in your project root `build.gradle` file:

```groovy
allprojects {
  repositories {
    jcenter() // should be there by default, and mavenCentral() should also work.
  }
}
```

**Kohii** has the following artifacts:

- `kohii-core`

This artifact contains core design of the library. It doesn't contain enough implementation detail to start the playback. To do so you need to include one of the following artifacts:

- `kohii-exoplayer`

This artifact contains the implementation of `kohii-core` that uses **ExoPlayer** as playback *backend*. As a result, you will also need to include necessary **ExoPlayer** artifacts as well. Your dependency to use `kohii-exoplayer` will look like below:

```groovy
def kohiiVersion = '1.0.0.2010004' // always use latest available version
def exoplayerVersion = '2.10.4'

implementation "im.ene.kohii:kohii-core:${kohiiVersion}"
implementation "im.ene.kohii:kohii-exoplayer:${kohiiVersion}"
implementation "com.google.android.exoplayer:exoplayer:${exoplayerVersion}"
```

!!! info "Regarding Kohii version name structure"
    **Kohii** extension for **ExoPlayer** uses a specific version of **ExoPlayer** for the playback logic. Because **ExoPlayer** doesn't guarantee backward compatibility on each minor release, **Kohii** includes the **ExoPlayer** release number in its release name to tell developer about which version of **ExoPlayer** it uses. For example **Kohii** `1.0.0.2010004` will use **ExoPlayer** `2.10.4` for playback logic, but **Kohii** `1.0.0.2011000` will use **ExoPlayer** `2.11.0` instead.

- `kohii-androidx`

This artifact contains the implementation of `kohii-core` that uses **AndroidX Media2** as playback *backend*. As a result, you will also need to include necessary **AndroidX Media2** artifacts as well. The setup would be the same as `kohii-exoplayer`, accept that you need these instead of **ExoPlayer**:

```groovy
def kohiiVersion = '1.0.0.2010004' // always use latest available version

implementation "im.ene.kohii:kohii-core:${kohiiVersion}"
implementation "im.ene.kohii:kohii-androidx:${kohiiVersion}"

def media2Version = '1.0.1'

implementation "androidx.media2:media2-widget:${media2Version}"
implementation "androidx.media2:media2-common:${media2Version}"
implementation "androidx.media2:media2-player:${media2Version}"
implementation "androidx.media2:media2-session:${media2Version}"
```

- `kohii-experiments`

This artifacts includes experimental implementations. In short the playback logic provided by this package should be used with caution. Most important things to mention are [1] **Kohii** doesn't guarantee the compatibility of the experiments, [2] Developers want to adopt these experiments are suggested to copy-paste and modify the code to meet their need.

Currently, `kohii-experiments` provides playback logic for **YouTube Playback** based on the Official [YouTube Android Player API](https://developers.google.com/youtube/android/player) and the Unofficial [YouTube Player library for Android and Chromecast](https://github.com/PierfrancescoSoffritti/android-youtube-player). Using this package as below:

```groovy
def kohiiVersion = '1.0.0.2010004' // always use latest available version

implementation "im.ene.kohii:kohii-core:${kohiiVersion}"
implementation "im.ene.kohii:kohii-experiments:${kohiiVersion}"
```

!!! warning
    Again, please use this package with caution!
