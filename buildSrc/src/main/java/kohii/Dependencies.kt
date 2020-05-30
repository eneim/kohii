/*
 * Copyright (c) 2019 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Versions {
  const val exoPlayerSnapShot = "dev-v2-SNAPSHOT"
  const val exoPlayer = "2.11.3"
  const val exoPlayerCode = 2011003
}

object ReleaseInfo {

  const val repoName = "kohii"
  const val groupId = "im.ene.kohii"
  const val description = "Android Video Playback made easy."
  const val websiteUrl = "https://github.com/eneim/kohii"
  const val vcsUrl = "https://github.com/eneim/kohii"
  val licenses = arrayOf("Apache-2.0")
}

@Suppress("MayBeConstant", "unused")
object BuildConfig {

  const val compileSdkVersion = 29
  const val targetSdkVersion = 29
  const val minSdkVersion = 19
  const val demoSdkVersion = 21 // to prevent dex limit on debug build.

  private val gitCommitHash = try {
    Runtime.getRuntime()
        .exec("git rev-parse --short HEAD")
        .inputStream.reader()
        .use { it.readText() }
        .trim()
  } catch (er: Exception) {
    "1.0.0"
  }

  private val gitCommitCount = 100 + try {
    Runtime.getRuntime()
        .exec("git rev-list --count HEAD")
        .inputStream.reader()
        .use { it.readText() }
        .trim().toInt()
  } catch (er: Exception) {
    0
  }

  val releaseVersionCode = gitCommitCount
  val releaseVersionName = "1.1.0.${Versions.exoPlayerCode}-A1"
}

@Suppress("MayBeConstant", "unused")
object Libs {

  object Common {
    const val androidGradlePlugin = "com.android.tools.build:gradle:4.0.0"
    const val dexcountGradlePlugin = "com.getkeepsafe.dexcount:dexcount-gradle-plugin:1.0.2"
    const val ktLintPlugin = "org.jlleitschuh.gradle:ktlint-gradle:9.2.1"
    const val bintrayPlugin = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"
    // 0.10.0 render method signature after the doc, which looks pretty bad.
    const val dokkaPlugin = "org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.18"

    val junit = "junit:junit:4.12"
    val junitExt = "androidx.test.ext:junit-ktx:1.1.1"
    val robolectric = "org.robolectric:robolectric:4.3.1"
    val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
  }

  object Kotlin {
    private const val version = "1.3.72"

    const val stdlibJdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"
    const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5"
  }

  object AndroidX {
    val activityKtx = "androidx.activity:activity-ktx:1.1.0"
    val appcompat = "androidx.appcompat:appcompat:1.1.0"
    val appcompatResources = "androidx.appcompat:appcompat-resources:1.1.0"
    val collectionKtx = "androidx.collection:collection-ktx:1.1.0"
    val benchmark = "androidx.benchmark:benchmark-junit4:1.0.0"
    val browser = "androidx.browser:browser:1.2.0"
    val palette = "androidx.palette:palette-ktx:1.0.0"
    val emoji = "androidx.emoji:emoji:1.0.0"

    val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
    val recyclerViewSelection = "androidx.recyclerview:recyclerview-selection:1.1.0-rc01"

    val viewPager = "androidx.viewpager:viewpager:1.0.0"
    val viewPager2 = "androidx.viewpager2:viewpager2:1.0.0"

    val coordinatorLayout = "androidx.coordinatorlayout:coordinatorlayout:1.1.0"

    object Media {
      private const val version = "1.0.3"
      // private const val version = "1.1.0-SNAPSHOT"
      val widget = "androidx.media2:media2-widget:$version"
      val common = "androidx.media2:media2-common:$version"
      val player = "androidx.media2:media2-player:$version"
      val session = "androidx.media2:media2-session:$version"
    }

    object Navigation {
      private const val version = "2.2.1"

      val runtimeKtx = "androidx.navigation:navigation-runtime-ktx:$version"
      val commonKtx = "androidx.navigation:navigation-common-ktx:$version"
      val uiKtx = "androidx.navigation:navigation-ui-ktx:$version"
      val fragmentKtx = "androidx.navigation:navigation-fragment-ktx:$version"
      val safeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:$version"
    }

    object Fragment {
      private const val version = "1.2.1"
      val fragment = "androidx.fragment:fragment:$version"
      val fragmentKtx = "androidx.fragment:fragment-ktx:$version"
    }

    object Test {
      private const val version = "1.2.0"
      val core = "androidx.test:core:$version"
      val runner = "androidx.test:runner:$version"
      val rules = "androidx.test:rules:$version"

      val espressoCore = "androidx.test.espresso:espresso-core:3.2.0-beta01"
    }

    val archCoreTesting = "androidx.arch.core:core-testing:2.1.0"

    object Paging {
      private const val version = "2.1.1"
      val common = "androidx.paging:paging-common-ktx:$version"
      val runtime = "androidx.paging:paging-runtime-ktx:$version"
    }

    val preference = "androidx.preference:preference-ktx:1.1.0"

    // beta4 breaks the overlay demo ...
    val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.0-beta3"

    val core = "androidx.core:core:1.2.0"
    val coreKtx = "androidx.core:core-ktx:1.2.0"

    object Lifecycle {
      private const val version = "2.2.0"
      val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
      val reactive = "androidx.lifecycle:lifecycle-reactivestreams-ktx:$version"
      val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
      val java8 = "androidx.lifecycle:lifecycle-common-java8:$version"
      val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
      val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
      val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
      val service = "androidx.lifecycle:lifecycle-service:$version"
    }

    object Room {
      private const val version = "2.2.3"
      val common = "androidx.room:room-common:$version"
      val runtime = "androidx.room:room-runtime:$version"
      val compiler = "androidx.room:room-compiler:$version"
      val ktx = "androidx.room:room-ktx:$version"
      val testing = "androidx.room:room-testing:$version"
    }

    object Work {
      private const val version = "2.3.1"
      val runtimeKtx = "androidx.work:work-runtime-ktx:$version"
    }
  }

  object ExoPlayer {
    private fun subLib(name: String): String {
      return "com.google.android.exoplayer:exoplayer-$name:${Versions.exoPlayer}"
    }

    private fun extLib(name: String): String {
      return "com.google.android.exoplayer:extension-$name:${Versions.exoPlayer}"
    }

    val all = "com.google.android.exoplayer:exoplayer:${Versions.exoPlayer}"

    val core = subLib("core")
    val ui = subLib("ui")
    val dash = subLib("dash")
    val hls = subLib("hls")
    val smoothStreaming = subLib("smoothstreaming")

    val mediaSession = extLib("mediasession")
    val okhttpExtension = extLib("okhttp")
    val imaExtension = extLib("ima")
    val rtmpExtension = extLib("rtmp")

    val ima = extLib("ima")
    val cast = extLib("cast")

    val workManager = extLib("workmanager")

    val allSnapshot = "com.github.google:exoplayer:${Versions.exoPlayerSnapShot}"
  }

  object Google {
    val material = "com.google.android.material:material:1.1.0"
    val gmsGoogleServices = "com.google.gms:google-services:4.3.2"
    val firebaseAnalytics = "com.google.firebase:firebase-analytics:17.2.2"
    val fabricPlugin = "io.fabric.tools:gradle:1.31.1"
    val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.10.1"

    val youtubeApi = "com.google.apis:google-api-services-youtube:v3-rev20190827-1.30.1"
    val httpClientAndroid = "com.google.http-client:google-http-client-android:1.34.1"
    val apiClientAndroid = "com.google.api-client:google-api-client-android:1.30.8"
    val apiClientGson = "com.google.api-client:google-api-client-gson:1.30.8"
  }

  object Dagger {
    private const val version = "2.26"
    val dagger = "com.google.dagger:dagger:$version"
    val androidSupport = "com.google.dagger:dagger-android-support:$version"
    val compiler = "com.google.dagger:dagger-compiler:$version"
    val androidProcessor = "com.google.dagger:dagger-android-processor:$version"
  }

  object Square {
    private const val moshiVersion = "1.9.2"
    val moshi = "com.squareup.moshi:moshi:${moshiVersion}"
    val moshiCodegen = "com.squareup.moshi:moshi-kotlin-codegen:${moshiVersion}"
    val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${moshiVersion}"
    val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.2"
    val okio = "com.squareup.okio:okio:2.4.3"
  }

  object Glide {
    private const val version = "4.11.0"
    val glide = "com.github.bumptech.glide:glide:$version"
    val compiler = "com.github.bumptech.glide:compiler:$version"
  }

  object Coil {
    private const val version = "0.11.0"
    val coilBase = "io.coil-kt:coil:$version"
    val coilSvg = "io.coil-kt:coil-svg:$version"
    val coilGif = "io.coil-kt:coil-gif:$version"
    val coilVideo = "io.coil-kt:coil-video:$version"
  }

  object Other {
    val androidSvg = "com.caverock:androidsvg-aar:1.4"
    val youtubePlayer = "com.pierfrancescosoffritti.androidyoutubeplayer:core:10.0.5"
    val timber = "com.jakewharton.timber:timber:4.7.1"
  }
}
