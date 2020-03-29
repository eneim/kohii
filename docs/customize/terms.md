# Kohii Components

## Introduction

This section helps you to understand the core components of **Kohii**.

## Renderer and Container

When play a Video to a destination `View`, we call that View a `renderer`. `VideoView` in Android framework, `PlayerView` in ExoPlayer are well-known `renderer`s. In **Kohii**, by calling `bind(someView)`, you are *placing* your Video to an *object* that can then contain a `renderer`. We call that *object* a `container`. In **Kohii**, we deal with `ViewGroup` as `container`, but theoretically it can be anything. In the future, we will try to expand the concept of `container` to other types. Also, a `renderer` can also be `container`, in which case we see the `renderer` is self-contained.

The ideas of **container** and **renderer** allow **Kohii** to build the abstraction where a `renderer` can be attached to `container`, detached from `container` and passed around, which allows `renderer`s to be reused for unlimited number of Videos. This is actually implemented in `kohii-exoplayer` already.

## Playback and Playable

**Playback** is a special object defined to manage a `container`. Like `Fragment` in Android framework which (optionally) has a `View` and *lifecycle*, `Playback`'s view is the `container`, and it has a lifecycle managed by a `Manager`. A Playback's lifecycle is scoped to the `container`'s lifecycle and the *Manager*'s lifecycle, which is as large as an Activity or Fragment lifecycle.

When you place a Video in a container by calling `bind`, **Kohii** allows you to have some configuration such as *tag*, *visible threshold* by which the Video should start or pause, etc. These configurations are passed to `Playback`, processed and passed down to lower layer to construct resource for an actual playback. So you can have a Video playing with one config in a `container`, but another config in another `container`. It is helpful when you switch a Video from list (where it plays with upto HD quality) to fullscreen (where the config allows it to play with full quality).

**Playable** is a special object defined to manage a `renderer`. A Playable is created when client wants to place a Video to a container by calling `bind`, and there is no previously created Playable for the same Video information. In other words, a Playable is created/retrieved when there is a Playback requests for it.

**Kohii** manages Playable globally, therefore its lifecycle is scoped to application lifetime. But once a Playable is no longer *needed*, it will be destroyed to save resource. In **Kohii**, when a Playable is not requested by any Playback, it will be considered *no longer needed* and will be scheduled to destroy. Though, during transient states like configuration change, current Playback will be destroyed (because its container is destroyed) and new Playback is still under construction, **Kohii** will give the Playable *a few more time* to wait for a Playback to request it. After this *a few more time*, if a Playable is not requested, **Kohii** will destroy it so system can reclaim its memory.

The ideas of **Playable** and **Playback** allow client to use the same Video and player resource for different destinations (a.k.a renderers), which then allows developers to build many complicated scenario more easier.

## Bucket, Manager and Group

Considering an application where you have an Activity contains 3 Fragments: A, B and C. A has a RecyclerView (named RA) contains a lot of Videos. B has a ScrollView (named SB) contains some Videos and another RecyclerView (named RB) contains many Videos too. You want only RB to start the Videos automatically, and later you want to disable the automatic playback of RB and enable it for RA. **Kohii** makes this possible by using a number of management components: Bucket, Manager and Group.

**Bucket** is designed to manage a *big* `View` like `RecyclerView` or `NestedScrollView`. Bucket has the responsibility to tell Manager about UI changes, as well as to select which Video to play from all Videos it *knows* about. For example a Bucket will know about Videos in its `RecyclerView`. To do so, Bucket has specific logic for each type of `View` to observe the scroll change, layout update, etc of that *big View* and notify the Manager about that change. Also, Bucket maintains references to all containers which are *staying* in the *big View*. For example, when you place a Video to a `FrameLayout` which is a child of a `ViewHolder` inside a `RecyclerView`, the Bucket for that `RecyclerView` will added a reference of the `FrameLayout` container to its own memory. So everytime it is asked to, Bucket will then fetch necessary information from those containers to decide which Video to play and which to pause.

**Manager** is designed to manage a `Fragment` or an `Activity` which contain *big Views*. Manager creates and manages Buckets for Views on demand. So in our scenario, Fragment B only need to register the `RecyclerView` to **Kohii**, the Manager will then acknowledge this `RecyclerView` and create a Bucket for it. Manager also manages all **Playbacks**, and closely communicate with **Group** regarding any UI change.

**Group** is as important as an Activity in Android framework. It contains many **Managers** just like an `Activity` contain many `Fragments`, and it listens to **Managers** request to refresh overall state. Because we will only allow a small number of videos from one *Bucket* of one *Manager* at a time to be playing, *Group* exists to take care of playback state of all *Managers*, and will carefully update the whole screen (i.e the `Activity`) on demand.

## Master, Engine

_COMING SOON_
