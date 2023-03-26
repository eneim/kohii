# Basic usages

## The scenario

This **basic usage** session will guide you step-by-step to complete this scenario: you have
a `Fragment` with many Videos in a vertical `RecyclerView`. You want each Video to start playing
automatically if that Video is *visible more than 65% of its full area, and stay on top of all the
visible Videos (fully, or partly)*. If you scroll the list, the Video that is not visible enough
will be paused automatically, and the other Video which sastisfy the condition above will start
playing automatically.

<img src="../art/kohii_demo_2.gif" width="216" style="display: block; margin: 0 auto;"/>

## TL,DR

We will explains a lot of details, so it may be a lot of texts. Here is the short version if you
want to start right away:

First, add this to your `Fragment#onViewCreated` or `Activity#onCreate`

=== "Kotlin"
    ```Kotlin
    val kohii = Kohii[this]
    kohii.register(this)
          .addBucket(recyclerView) // assume that you are using a RecyclerView
          .addBucket(anotherRecyclerView) // yeah, 2 RVs in one place, why not.
    ```

=== "Java"
    ```Java
    Kohii kohii = Kohii.get(this);
    kohii.register(this)
          .addBucket(recyclerView) // assume that you are using a RecyclerView
          .addBucket(anotherRecyclerView); // yeah, 2 RVs in one place, why not.
    ```

Second, add this in your `RecyclerView.Adapter#onBindViewHolder`, or corresponding place
in `ViewHolder`

=== "Kotlin"
    ```Kotlin
    // You need to pass the kohii instance here
    kohii.setUp(videoUrl).bind(playerView)
    ```

=== "Java"
    ```Java
    // You need to pass the kohii instance here
    kohii.setUp(videoUrl).bind(playerView);
    ```

Done, you have what you want. But before leaving, let's discover the details below with your
curiousity.

## Before you start: thinking in Kohii

It is important that you get the concept of **Kohii** before we go further. Because the way you
think about Video playback until now would be different to what **Kohii** thinks.

Until now, you may see and/or use the following pattern:

```Kotlin
videoView.setVideoPath(videoUrl)
```

This line *reads*:

> I have a VideoView and I will play a Video in it by calling `setVideoPath` method.

In **Kohii**, the *direction* is opposite:

```Kotlin
kohii.setUp(videoUrl).bind(videoView)
```

This line reads:

> I have a Video and I will play it in a VideoView. Let's setup the Video and bind it to the
> VideoView.

The difference here is: who is the **main actor**? In traditional way, the `VideoView` *owns* the
Video and therefore, when it dies, we also lose the Video playback. In **Kohii**, we let the Video
be the active part. It *acknowledges* the `VideoView` it will *be played* on. So when
the `VideoView` dies, your Video can be smoothly switched to other `VideoView`.

To give you an imagine about why it is good this way, consider this scenario: you have a list of
Videos, and you want to open one Video in fullscreen, ***smoothly***.

Thinking in _traditional way_: How I can open this `VideoView` (which is now in the list) in
fullscreen? If I use other `VideoView` I will need to call that `setVideoPath` again and it will
create new resource and stuff and the playback will be reloaded, and so on... We see some challenges
here already.

While thinking in _**Kohii** way_, it sounds easier: How can I open this Video (which is now in the
list) in fullscreen? Can I just ***switch*** it from current `VideoView` to the
fullscreen `VideoView`?

This way of thinking is the base for all the abstractions in **Kohii**. How this idea comes to life
will be discussed later.

Now that you have the concept about **Kohii**, let's get our hands dirty.

## Preparation

This section will use some assumptions below:

- You will use **Kohii** in `Fragment` (the usage will be the same in `Activity`).
- You use **ExoPlayer** for the playback (implementation provided via `kohii-exoplayer` extension).
- You own the content or have enough rights to use them. **Kohii** has no responsibility about how
  you use it in your app.

First, you need to initialize a few things so that **Kohii** makes sense of your application.

■ Initialize **Kohii** instance

**Kohii** should be used as a Global instance. You can get it as follow:

=== "Kotlin"
    ```Kotlin
    val kohii = Kohii[this@Fragment]
    // or using Context also works: val kohii = Kohii[requireContext()]
    ```

=== "Java"
    ```Java
    Kohii kohii = Kohii.get(this);
    // or using Context: Kohii kohii = Kohii.get(requireContext());
    ```

As a *singleton*, **Kohii** instance can be passed around or re-obtained in other `Fragment`. You
can also use *dependency injection library* like **Dagger** to prepare a global instance, and inject
it to required places.

■ Register necessary objects to **Kohii**

**Kohii** needs to know about a few things to work properly:

- Where you are using **Kohii** from? A `Fragment` or `Activity`? Line below answers that question:

=== "Kotlin"
    ```Kotlin
    // From the Fragment's onViewCreated()
    kohii.register(this@Fragment)
    ```

=== "Java"
    ```Java
    // From the Fragment's onViewCreated()
    kohii.register(this);
    ```

The line above also return a [`Manager`](glossary.md#bucket-manager-and-group) object. It is useful
in some advance usages, but we don't need it for now.

- Which ViewGroup contains Videos?

We call that ViewGroup a [*Bucket*](glossary.md#bucket-manager-and-group). Because you may have more
than one *bucket* in your `Fragment`, and not all of them need to be tracked by **Kohii**, you
should only register ones you care about. Code for it is as below:

=== "Kotlin"
    ```Kotlin
    kohii.register(this@Fragment) // or manager
      .addBucket(recyclerView)
      .addBucket(anotherRecyclerView) // yeah, 2 RVs in one Fragment, why not.
    ```

=== "Java"
    ```Java
    kohii.register(this) // or manager
      .addBucket(recyclerView)
      .addBucket(anotherRecyclerView); // yeah, 2 RVs in one Fragment, why not.
    ```

It is enough setting up for this session. Next, we will setup the Video for each ViewHolder.

## Integration

To make it works, you need only one line:

=== "Kotlin"
    ```Kotlin
    // You must pass the kohii instance here
    // playerView is the PlayerView you want to play your Video on
    kohii.setUp(videoUrl).bind(playerView)
    ```

=== "Java"
    ```Java
    // You must pass the kohii instance here
    // playerView is the PlayerView you want to play your Video on
    kohii.setUp(videoUrl).bind(playerView);
    ```

But let's understand the concept behind:

In the one line above: `kohii.setUp(videoUrl)` turns the url to
a [`Binder`](../api/kohii-core/kohii.v1.core/-binder/) object which can be used to bind to
a [`container`](glossary.md#renderer-and-container). Once you finish the setup, you have the Video
to be automatically played/paused once user scrolls the list such that the `container` is visible
more (will play) or less (will pause) than 65% of its area.

Also, to ensure the playback is automatic, if the [`renderer`](glossary.md#renderer-and-container)
is a `PlayerView` **Kohii** will forcefully disable the `PlayerView`'s `PlayerControlView` even if
you set it before. To have manual playback control enabled, you need some additional configuration
which will be discussed in other session.
