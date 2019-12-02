# Common usages of Kohii

## Required: common setup for Kohii

Assuming that you are using **Kohii** in a Fragment ``MyFragment``, having a ViewGroup to contains some Videos. Below steps are to initialize **Kohii** before going any further.

■ Initialize **Kohii** instance. It will be global and unique in your application lifecycle.

```kotlin
val kohii = Kohii[this@MyFragment]
```

■ Register current Fragment as the ``PlaybackManager``. In **Kohii**, ``PlaybackManager`` helps manage all Videos in a Fragment or Activity. When using with ``ViewPager`` whose pages are ``Fragment``s, we need to register both the parent ``Fragment`` (who owns the ``ViewPager``) and child ``Fragment`` (who is page of the ``ViewPager``). 

```kotlin
// Calling this will return a ``PlaybackManager`` instance
val manager = kohii.register(this@MyFragment)
```

■ Register the ViewGroup that contains the Videos inside. It may or may not be the direct parent of the Video View. In **Kohii**, a ``View`` that is used to render a Video playback is called ``Target`` and the ``ViewGroup`` which is the parent of a ``Target`` is called *host*. To help the ViewGroup *watch* its Targets, **Kohii** has the ``TargetHost`` to wrap this ViewGroup. Here is how we create a ``TargetHost`` by registering the ``ViewGroup``:

```kotlin
// This returns a nullable TargetHost instance
// The result is null when the View is already registered before, not null otherwise.
val host = manager.registerTargetHost(videoHostViewGroup)
```
That is enough for a common usage. In practice, your setup will look like below:

```kotlin
// Setup **Kohii** from Fragment's onViewCreated or Activity's onCreate method.
override fun onViewCreated(
  view: View,
  savedInstanceState: Bundle?
) {
  super.onViewCreated(view, savedInstanceState)
  val kohii = Kohii[this@MyFragment]
  val manager = kohii.register(this@MyFragment)
  val host = manager.registerTargetHost(videoHostViewGroup)!!
  
  // TODO anything else
}
```

In basic usage, you do not need to get returned value of the setting above:

```kotlin
// Setup **Kohii** from Fragment's onViewCreated or Activity's onCreate method.
override fun onViewCreated(
  view: View,
  savedInstanceState: Bundle?
) {
  super.onViewCreated(view, savedInstanceState)
  Kohii[this@MyFragment].also { kohii ->
    kohii.register(this@MyFragment, videoHostViewGroup) // an overloaded version of register().
  }
  
  // TODO anything else
}
```  

## Usages

#### 1.0 One video to one existing PlayerView

> In **Kohii**, the term *binding* stands for the act in that a Video resource provided by client being converted to playable resource (eg: a Uri to a MediaSource object), and then being prepared to the Surface to play the Video). It is just as simple concept as ``videoView.setVideoPath(videoUri)``, but more powerful behavior.

This is the most common/simple usage of **Kohii**. Assuming that you have an uri, and a ``PlayerView`` to show the Video named ``playerView``, below is how to do it:

```kotlin
kohii.setUp(uri).bind(playerView)
```

Note that, the [**required**](#required-common-setup-for-kohii) steps above must be done first. Or else there will be no ``TargetHost`` to watch the ``PlayerView``, and your client will throw exception.

With that *one line*, you will have the following done:

- The Uri you provided will be wrapped in a ``MediaItem`` object, and then be used to create a ``Playable`` object. This object has a ``Bridge`` object inside it contains all necessary resources for a Video playback: factory to build ``MediaSource`` from the ``Uri``, pool to acquire ``Player`` instance for the playback, all preparation logic are done automatically and will wait for the trigger to start or pause the Video.

- A ``Playback`` object is created using the ``Playable`` above, and is added to the ``PlaybackManager`` we created before. This ``PlaybackManager`` will find the correct ``TargetHost`` for the ``PlayerView``, listen to its behavior (scrolled, attached, detached, etc) to trigger the playback start/pause.

From this point onward, you have the Video to be automatically played/paused once user scrolls the list such that the ``PlayerView`` is visible more (will play) or less (will pause) than 65% of its area.

Also, to ensure the playback is automatic, **Kohii** will forcefully disable the ``PlayerView``'s ``PlayerControlView`` even if you set it before. To have manual playback control enabled, you need some additional configuration which will be discussed in next usage.

#### 1.1 Same as 1.0, with listeners

In practice, client wants to observe the status of the Playback. **Kohii** provides a listener system to fulfill this requirement.

A ``Playback`` has the following important listeners you can use:

<details>
  <summary>Playback.Callback: listen to status of a Playback object:</summary>

```kotlin
interface Callback {

  /** Called when the Playback is added to the PlaybackManager */
  fun onAdded(playback: Playback<*>) {}

  /** Called when the Playback becomes active. It is equal to that the target PlayerView is attached to the Window */
  fun onActive(playback: Playback<*>) {}

  /** Called when the Playback becomes inactive. It is equal to that the target PlayerView is detached from the Window */
  fun onInActive(playback: Playback<*>) {}

  /** Called when the Playback is removed from the PlaybackManager */
  fun onRemoved(playback: Playback<*>) {}
}
```
</details>

<details>
  <summary>PlaybackEventListener: listen to play/pause/end event of a playback:</summary>

```kotlin
interface PlaybackEventListener {

  /** Called when a Video is rendered on the Surface for the first time */
  fun onFirstFrameRendered(playback: Playback<*>) {}

  /**
   * Called when buffering status of the playback is changed.
   *
   * @param playWhenReady true if the Video will start playing once buffered enough, false otherwise.
   */
  fun onBuffering(
    playback: Playback<*>,
    playWhenReady: Boolean
  ) {
  } // ExoPlayer state: 2

  /** Called when the Video starts playing */
  fun onPlay(playback: Playback<*>) {} // ExoPlayer state: 3, play flag: true

  /** Called when the Video is paused */
  fun onPause(playback: Playback<*>) {} // ExoPlayer state: 3, play flag: false

  /** Called when the Video finishes its playback */
  fun onEnd(playback: Playback<*>) {} // ExoPlayer state: 4

  /** Called when right before Playback's play() method is called. Maybe called multiple times */
  fun beforePlay(playback: Playback<*>) {}

  /** Called when right after Playback's pause() method is called. Maybe called multiple times */
  fun afterPause(playback: Playback<*>) {}
}
```
</details>

Before going into the actual setup, let's take a look at what we have done and what we got.

First, the ``bind`` method above has the full signature as below:

```kotlin
fun <CONTAINER : Any> bind(
  target: CONTAINER,
  callback: ((Playback<OUTPUT>) -> Unit)? = null
)
```

Here you can see a callback that is null by default. This callback send back to client the ``Playback`` instance that was created by the setup. Using this ``Playback``, client can have further setup, like adding listeners.

Second, calling ``kohii.setUp(uri)`` will return an important object: a ``Binder``.

``Binder`` object is the builder of a Playable and can be used to *bind* a Playable to a Target. Being a builder, it allows client to give more configuration via ``Binder.with(Params)`` method. You can call it in *Kotlin DSL* style like below:

```kotlin
kohii.setUp(uri).with {
  tag = uniqueTag
  repeatMode = REPEAT_MODE_ONE
} // this method returns the Binder object
```

The ``bind`` method above is an instance method of ``Binder`` object. ``Binder`` allows ``Playback.Callback`` as a parameter. This is because this callback send the signal of a ``Playback`` being added to the manager. This event happens before the callback in ``bind`` method returns. Therefore, to be able to listen to the ``onAdded`` callback, ``Playback.Callback`` need to be added earlier.

On the other hand, the ``PlaybackEventListener`` can be added at any time. Let's take a look at the setup below:

```kotlin
kohii.setUp(uri)  // this return a Binder
  .with {
    callback = myCallback
  } // the same Binder, now with callback installed
  .bind(playerView) { playback ->
    playback.addPlaybackEventListener(playbackListener)
  }
```

With this setup, your ``myCallback`` will receive ``onAdded`` callback as expected. Also, it's worth notice that adding ``PlaybackEventListener`` will allow the client to receive the signal immediately. It means that, if the Video is playing, and client call ``playback.addPlaybackEventListener(playbackListener)``, its ``onPlay()`` method will be called immediately.

This allow client to response to the Playback state as soon as possible. For example showing or hidding thumbnail on relevent situations.

#### 2.0 Setup Videos in NestedScrollView/~~ScrollView~~

**Kohii** support every possible ``ViewGroup``, including ``NestScrollView`` or ``ScrollView``. It's worth mention that ``NestedScrollView`` is superior to ``ScrollView``, and is updated more frequently, so I recommend to use ``NestedScrollView`` instead. Below setup assumes the host is ``NestedScrollView``.

The same steps as [before](#required-common-setup-for-kohii) are required.

As ``NestedScrollView`` do not recycle its child like ``RecyclerView``, for each PlayerView we just need to repeat the setup for one Video:

```kotlin
// videosToViews is of type Collection<Pair<Uri, PlayerView>>
videosToViews.forEach { (uri, playerView) -> 
  kohii.setUp(uri).bind(playerView)
}
```

(I use collection of Pairs just for demo purpose. It should be designed carefully in practice).

With this setup, you will have what is done for one Video in one PlayerView, plus:

- PlayerView will only prepare the resource (setup Player instance, prepare MediaSource, etc) when it is partly or fully visible to user.
- When a PlayerView is scrolled off screen, Kohii will automatically release its resource.

This means that, **even if you setup for 100 Uris to 100 PlayerViews, only those are visible will be active and consume resource (networking, Video decoding, etc)**.

#### 3.0 Setup Videos in RecyclerView

Setting up Videos in ``RecyclerView`` is pretty much the same with ``NestedScrollView``: just setup the uri whenever you have one for a PlayerView. But because ``RecyclerView`` automatically recycle its Views, you don't need to worry about anything.

#### 4.0 Setup Videos in ViewPager whose pages are Fragments
