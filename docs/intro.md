## TL, DR

**Kohii** is a library that:

- Is built for Android, supports Android 4.4 (SDK 19) and up.
- Provides easy to use Video playback functionality, including automatic playback, playback
  continuity across config change, lifecycle change and more.
- By default, support ExoPlayer and any ViewGroup as the *bucket* for videos, including:
  RecyclerView, NestedScrollView, ViewPager, ViewPager2, general ViewGroup, etc

Kohii provides following features by default:

- [x] Full automatic playback control using ExoPlayer as playback framework.
- [x] Semi manual playback control using ExoPlayer as playback framework. User can play/pause
  specific player on demand, Kohii will smartly pause the Video if it is not visible anymore, and
  restore the previous state later if needed.
- [x] Full manual playback control using ExoPlayer as playback framework. User can play/pause
  specific player on demand, Kohii does not touch anything, but take care of cleaning resource if
  needed (eg: release Player instances after user closes the Application).
- [x] Preserve *playback continuity* across configuration changes, lifecycle changes (eg: from
  Activity to Activity) using ExoPlayer as playback framework. This means: rotating the device,
  switching to multi-windows mode or opening full-screen player doesn\'t trigger pausing/resuming
  the playback. While the visual part will be re-rendered (due to View recreation), the audio part
  is kept smoothly, provide continuous playback experience.
- [x] Reuse PlayerView instances for many players. Thinking that playing thousands of videos in
  sequence just using ***one PlayerView instance***.
- [x] Scoped volume configuration. This means: you can set volume value of one Video, and apply the
  same for others, depending on the scope you want. The scope can be just that Video, or all
  Videos *in the same RecyclerView/ScrollView*, or all Videos *in the same Activity*.
- [x] Rich features demo app: demo and guidelines for creating various UX/UI patterns, from simple
  to complicated, including Picture-In-Picture playback or Scroll-To-Mini-Player UX just like
  YouTube app.

For advance developers

- **Kohii** API is highly abstracted. You can easily extend it to support non-ExoPlayer API, or more
  complicated playback API. There is demo on how to
  use [this YouTube player library](https://github.com/PierfrancescoSoffritti/android-youtube-player)
  in Kohii.

## The idea of Playback Continuity

The concept *Playback Continuity* comes when I prepared
for [the Tokyo DroidKaigi 2019](https://droidkaigi.jp/2019/timetable/70957/) where I will also talk
about the same topic. Its idea was in my mind for a long time ago.

Thinking about a Video playback, what are the common scenarios:

- Single, individual playback, like watching a movie on Netflix or YouTube.
- Multiple playback, like viewing many videos on a timeline like Facebook, and watch them one by one
  while scrolling through the list.

All of these scenarios must share some common behaviors:

## Features

#### 0. Rich feature demo application

As always, I try to cover as much of the capabilities of the library as possible in the demo app. By
that, you can have the recommended ways to implement many patterns using **Kohii**, including:

- Using Kohii in RecyclerView only (the simplest pattern).
- Using Kohii in *NestedScrollView* only (yes, **Kohii** does not just support RecyclerView, it
  works with any *ViewGroup* you can imagine of, or, most of them).
- Using Kohii in *ViewPager* only, where each page is a *Fragment* (this is one of the thing that **
  toro** could not support well, so Kohii is here for the rescue).
- Using Kohii in Master/Detail UI composition (this is a new pattern I\'m experimenting with, and it
  needs time to be battle proof, and it needs your feedback to be mature <3).
- Using Kohii in RecyclerView, with 'click-to-fullscreen' feature like YouTube UI. This is the game
  changer, my **selling point for Kohii** <3. Implementation of it is also discussed
  in [this post](https://ene.im/2019/02/10/droidkaigi-2019-part-4/).
- Using Kohii in RecyclerView, with 'click-to-fullscreen' feature that open the fullscreen playback
  in new **Activity**, *without pausing/resuming the playback*. So starting new Activity for
  fullscreen player will not interrupt your playback UX, sounds good huh?
- Using Kohii in RecyclerView, with 'click-to-fullscreen' feature that open the fullscreen playback
  in new **Fragment**, because *single Activity* is the new trend. To make it more fun, I also add
  the *fragment transition learnt
  from [here](https://github.com/google/android-transition-examples/tree/master/GridToPager)*.
- And more to come, depending on the feedback I got, request I receive and time I have to spend.

#### 1. Few-line setup, full auto

Thinking that you just need the following lines to integrate fully automatic Video playback in to
your App:

```kotlin
// Init Kohii for Fragment or Activity
val kohii = Kohii[this /* fragment or activity instance */]
kohii.register(this, arrayOf(recyclerView)) // register the RecyclerView to be a 'host' of our Videos.

// In ViewHolder or Adapter's onBindViewHolder, where kohii instance is passed from above
kohii.setUp(videoUrl).bind(playerView) // playerView is instance of ExoPlayer's PlayerView.
```

This is the fastest way to use **Kohii**. With this setup, you got:

- Videos will be played/paused automatically on user's scroll.
- The top-most PlayerView whose visible area is equal to, or more than **65%** of its area will be
  the one to play, all other PlayerView instances will be paused.
- Configuration changes like multi-windows mode, orientation change etc will not interupt the
  playback. (As the matter of fact, SurfaceView or TextureView used in PlayerView will be recreated,
  so you will see the *glitch* of the Video image frame, but the Audio will keep playing during the
  configuration change).

Under the hood:

- Resource for Video playback is initialized as late as possible, so there is little to no impact to
  app's UX.
- Resource for Video playback is cached for reusability, so playback switching will be as fast as
  possible.
- Cached resource will be released as soon as possible, so there is no concern about memory leak.
- All playback logic, ExoPlayer implementation are provided by Kohii. You don't need to worry about
  anything.

Digging in the API, you will have more control over the playback behavior, including:

- Repeat mode, which is the same with that of ExoPlayer.
- Delay, which help you to start the Playback with an amount of delay.
- Visible area of the PlayerView that trigger the playback. It is 65% by default, but you are free
  to change it.
- Initial playback info, so you can control the starting position, volume of a playback.
- And more.

#### 2. Reusing PlayerView instance, with the same amount of code

Yes, you read it correctly. Now you can reuse PlayerView instance across many Video playback. This
is something **toro** could not archive, due to the limitation of its design. And as it
is [highly requested](https://github.com/google/ExoPlayer/issues/867), **Kohii** is my answer to the
problem.

Changing the setup before to this

```kotlin
// Init Kohii for Fragment or Activity
val kohii = Kohii[this /* fragment or activity instance */]
kohii.register(this, arrayOf(recyclerView)) // register the RecyclerView to be a 'host' of our Videos.

// In ViewHolder or Adapter's onBindViewHolder, where kohii instance is passed from above
kohii.setUp(videoUrl).bind(playerViewContainer) // playerViewContainer is a ViewGroup to put PlayerView instance on demand.
```

Just changing from using a **PlayerView** instance directly to using a *container*, you are done. To
help you understand this feature, let\'s talk about the *why* real quick: *why reusing PlayerView
instance is that cool?*.

If you take a look on [the issue I mention above]((https://github.com/google/ExoPlayer/issues/867)),
go from bottom-up because it is a long issue to read through, you can see the discussion about
reusing stuff in the *ExoPlayer & RecyclerView combination*. Yes, it is important to reuse as much
as possible, to keep a smooth user experience, as well as to save memory.

Assuming that you use PlayerView in your ViewHolder to play the video. If your viewport has 3 ~ 4
ViewHolders that contains Video, there will be at most one PlayerView that is playing at a time, and
at least 2 ~ 3 PlayerView instances doing nothing, but still keeping an expensive *Surface object*
alive. We know that creating and keeping Surface are expensive operations, so reusing the Surface is
a reasonable requirement.

How **Kohii** supports this:

- The required step is client provides the *container* to Kohii using the code above. The *
  container* can be just a FrameLayout.
- Kohii keeps a pool of **PlayerView** that will create the PlayerView instance on demand, and keep
  the cache of up to 2 instances of it, in the Activity's *lifecycle* scope. So once the Activity is
  destroyed, the pool is automatically cleared, again, no fear of memory leak.
- Kohii knows *when the PlayerView should appear, and when it should not*. When the PlayerView is
  demanded, Kohii acquires one instance from the pool. If there is no cached instance, a new
  instance will be created. On one hand, this instance will be passed to the playback engine, and on
  the other hand, it will be added to the *container* by internal mechanism. When the PlayerView
  should disappear, Kohii will remove it from the *container*, and release it back to the pool for
  reuse.
- Because there will be at most one PlayerView to play at a time, and Kohii's pool keeps up to 2
  instances at a time, there will be at most 2 PlayerView instances living at a moment. (In
  practice, depends on how complicated your video content is, there will be more. For example you
  have both DRM content and non-DRM content, and you want to use TextureView for non-DRM ones, then
  Kohii keeps 2 pools for 2 types of playback).

The caveat of this mechanism is that *by default* you do not control the way a PlayerView is
created (spoiler: with more lines of code, you can).

With more advance setup, you have the following control:

- How to add and remove PlayerView instance to/from the *container*. Because your container may
  contains other Views, Kohii allows you to provide your way to handle this operation.
- How to create PlayerView instance, because you may have your custom PlayerView implementation.

#### 3. Manual control

Some of the requirement I got from **toro** are the abilities to:

- Manually control the playback using UI/buttons
- Mute/Un-mute one playback and apply that to all

Those 2 control can be gathered in one group: manual playback control, with scope.

**Kohii** provides the following behaviors to make it possible:

- Allow client to configure so that, user can fully control the video playback, without any help by
  Kohii. For instance: when your RecyclerView is shown, and the first ViewHolder is a Video and it
  is fully visible. This behavior will not start the playback automatically, user can start/pause/do
  anything with the playback using client-provide UI controller. Once user scrolls the Video so that
  it is no longer visible on screen, Kohii will not do anything to pause or release the playback. If
  it was playing before, it keeps playing. If it was paused before, it keeps being paused. But once
  the Activity that contains this playback is destroyed, Kohii will correctly clear any resource in
  use. And even better, the manual playback is retained across configuration change, without any
  interuption.

- Allow client to configure so that, user can control the playback on the fly. This means that:
  Kohii will still start the playback automatically, but once user click to some buttons to pause
  it, Kohii will not change that state. So a video paused by user, even if it is fully visible, will
  not be played automatically (in full-auto config, Kohii will start this playback). When user
  scrolls the Video so that it is no longer visible enough, Kohii will do its jobs to pause and
  release resource if needed. And when user scrolls the Video back to visible enough, Kohii will put
  the playback to its previous state: if it was playing before being scrolled off-screen, it will be
  resumed; if it was paused before being scrolled off-screen, it keeps being paused.

- About the volume control, it works in a bit different way: Kohii allows client to apply a volume
  value by *scope*. There are 4 scopes in Kohii, but to not go too deep, I will just talk about 2:
  playback scope, and global scope. If client applies a volume value to *playback scope*, then only
  the receiver playback object will have this new volume value. But if client applies a volume value
  to *global scope*, then all currently playing playbacks, and any playback that *will be created
  next* will have the new volume value. The control flow is easy to imagine, but not easy to
  implementation. What I did for Kohii is one of my best thing I have ever done. And I hope it helps
  you to solve your problems.
