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

@Suppress("unused")
object Versions {
  const val exoPlayerSnapShot = "dev-v2-SNAPSHOT"
  const val exoPlayer = "2.9.6"
  const val exoPlayerCode = 2906
}

@Suppress("MayBeConstant", "unused")
object BuildConfig {

  const val compileSdkVersion = 28
  const val targetSdkVersion = 28
  const val minSdkVersion = 19
  const val demoSdkVersion = 21 // to prevent dex limit on debug build.

  private val gitCommitHash = Runtime.getRuntime()
      .exec("git rev-parse --short HEAD")
      .inputStream.reader()
      .use { it.readText() }
      .trim()
  private val gitCommitCount = 100 + Runtime.getRuntime().exec("git rev-list --count HEAD")
      .inputStream.reader().use { it.readText() }.trim().toInt()

  val releaseVersionCode = gitCommitCount
  val releaseVersionName = "1.0.0.${Versions.exoPlayerCode}-A11"
}

@Suppress("MayBeConstant", "unused")
object Libs {

  object Common {
    const val androidGradlePlugin = "com.android.tools.build:gradle:3.5.0-beta05"
    const val dexcountGradlePlugin = "com.getkeepsafe.dexcount:dexcount-gradle-plugin:0.8.6"
    const val ktLintPlugin = "org.jlleitschuh.gradle:ktlint-gradle:7.2.1"

    val junit = "junit:junit:4.12"
    val robolectric = "org.robolectric:robolectric:4.3"
    val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
  }

  object Kotlin {
    private const val version = "1.3.40"

    const val stdlibJdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"
    const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"
  }

  object AndroidX {
    val appcompat = "androidx.appcompat:appcompat:1.1.0-beta01"
    val appcompatResources = "androidx.appcompat:appcompat-resources:1.1.0-beta01"
    val collectionKtx = "androidx.collection:collection-ktx:1.1.0"
    val browser = "androidx.browser:browser:1.0.0"
    val palette = "androidx.palette:palette:1.0.0"
    val emoji = "androidx.emoji:emoji:1.0.0"

    val recyclerView = "androidx.recyclerview:recyclerview:1.1.0-alpha06"
    val recyclerViewSelection = "androidx.recyclerview:recyclerview-selection:1.1.0-alpha06"

    val viewPager = "androidx.viewpager:viewpager:1.0.0"
    val viewPager2 = "androidx.viewpager2:viewpager2:1.0.0-alpha05"

    object Navigation {
      private const val version = "2.1.0-alpha05"

      val runtimeKtx = "androidx.navigation:navigation-runtime-ktx:$version"
      val commonKtx = "androidx.navigation:navigation-common-ktx:$version"
      val uiKtx = "androidx.navigation:navigation-ui-ktx:$version"
      val fragmentKtx = "androidx.navigation:navigation-fragment-ktx:$version"
      val safeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:$version"
    }

    object Fragment {
      private const val version = "1.1.0-beta01"
      val fragment = "androidx.fragment:fragment:$version"
      val fragmentKtx = "androidx.fragment:fragment-ktx:$version"
    }

    object Test {
      private const val version = "1.2.0"
      val core = "androidx.test:core:$version"
      val runner = "androidx.test:runner:$version"
      val rules = "androidx.test:rules:$version"

      val espressoCore = "androidx.test.espresso:espresso-core:3.2.0"
    }

    val archCoreTesting = "androidx.arch.core:core-testing:2.0.1"

    object Paging {
      private const val version = "2.1.0"
      val common = "androidx.paging:paging-common:$version"
      val runtime = "androidx.paging:paging-runtime:$version"
    }

    val preference = "androidx.preference:preference:1.1.0-beta01"

    val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.0-beta2"

    val coreKtx = "androidx.core:core-ktx:1.2.0-alpha02"

    object Lifecycle {
      private const val version = "2.2.0-alpha01"
      val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
      val reactive = "androidx.lifecycle:lifecycle-reactivestreams-ktx:$version"
      val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
      val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
      val service = "androidx.lifecycle:lifecycle-service:$version"
    }

    object Room {
      private const val version = "2.1.0"
      val common = "androidx.room:room-common:$version"
      val runtime = "androidx.room:room-runtime:$version"
      val compiler = "androidx.room:room-compiler:$version"
      val ktx = "androidx.room:room-ktx:$version"
      val testing = "androidx.room:room-testing:$version"
    }

    object Work {
      private const val version = "2.0.1"
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

    val okhttpExtension = extLib("okhttp")
    val imaExtension = extLib("ima")
    val rtmpExtension = extLib("rtmp")
  }

  object Google {
    val material = "com.google.android.material:material:1.1.0-alpha07"
    val gmsGoogleServices = "com.google.gms:google-services:4.3.0"
    val firebaseCore = "com.google.firebase:firebase-core:17.0.0"
    val fabricPlugin = "io.fabric.tools:gradle:1.29.0"
    val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.10.1"
  }

  object Dagger {
    private const val version = "2.23.2"
    val dagger = "com.google.dagger:dagger:$version"
    val androidSupport = "com.google.dagger:dagger-android-support:$version"
    val compiler = "com.google.dagger:dagger-compiler:$version"
    val androidProcessor = "com.google.dagger:dagger-android-processor:$version"
  }

  object Glide {
    private const val version = "4.9.0"
    val glide = "com.github.bumptech.glide:glide:$version"
    val compiler = "com.github.bumptech.glide:compiler:$version"
  }
}
