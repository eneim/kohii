# Glossary

This section helps you to understand the core concepts of **Kohii**.

## Renderer and Container

When we play a Video to a destination surface, we call that surface a `Renderer`. Some examples of a
Renderer are `VideoView` in Android framework, `PlayerView` in ExoPlayer.

A component that can contain a `Renderer` is called `Container`. If a `PlayerView` renderer is added
directly to a `FrameLayout`, that `FrameLayout` is a container. In **Kohii**, by
calling `bind(someView)`, you are *placing* your Video to a `Container`. The renderer can also be
the container when it _contains_ itself.

!!! Note
Currently in **Kohii**, we only support `ViewGroup` containers, but theoretically it can be
anything. In the future, we will try to expand the concept of container to other types.

The idea of **Renderer** and **Container** allows **Kohii** to build the abstractions around them
where a renderer can be attached to a container, detached from a container and passed around, and
therefore renderers can be reused for unlimited number of Videos. This behavior is supported out of
the box by `kohii-exoplayer` and `kohii-androidx` packages.

## Playback and Playable

**Playback** is a component in **Kohii**, designed to manage the container. Like **Fragment** in the
Android framework which (optionally) has a `View` and `lifecycle`, **Playback** manages a view which
is the container, and it has a lifecycle controlled by another component called **Manager**. A
Playback's lifecycle is scoped to the container's lifecycle and the Manager's lifecycle, which is as
large as an Activity or a Fragment's lifecycle.

When you place a Video in a container by calling `bind(container)`, **Kohii** allows you to have
some configurations such as tag, delay, visible threshold by which the Video should start or pause,
etc. These configurations are passed to the Playback, processed and passed down to lower layer to
construct resources for an actual playback. So you can have a Video playing with one configuration
in a container, but another configuration in another container. It is helpful when you want to
switch a Video playback from list to full-screen and vice-versa.

**Playable** is a component in **Kohii**, designed to manage the renderer. A Playable is created
when the client calls `bind(container)`, and there is no Playable created previously for the same
Video information. In other words, a Playable will be available when there is a Playback requests
it.

**Kohii** manages Playable globally, which means that its lifecycle is scoped to the Application
lifetime. But once a Playable is no longer *needed*, it will be destroyed. In **Kohii**, when a
Playable is not used by any Playback, it will be released and scheduled to be destroyed. Though,
during transient states like configuration change, current Playback will be destroyed (because its
container is destroyed) and new Playback is still under construction, **Kohii** will give the
Playable *a few more time* to wait for a Playback to request it. After this duration, if a Playable
is not requested, **Kohii** will destroy it to reclaim its memory.

The idea of **Playable** and **Playback** allows client to use the same Video and player resource
for different renderers, which help to reduce the memory usage and enable developers to build
complicated scenarios much easier.

## Bucket, Manager and Group

Considering an Application where you have an Activity contains 3 Fragments: A, B and C. A has a
RecyclerView (named RA) contains a lot of Videos. B has a ScrollView (named SB) contains many Videos
and another RecyclerView (named RB) contains many Videos too. You only want RB to start playing the
Videos automatically, and later you want to disable the Video playback of RB and enable it for
RA. **Kohii** makes this possible by using a number of management components: Bucket, Manager and
Group.

**Bucket** is a component designed to manage a *big* `View` component like `RecyclerView`
, `NestedScrollView` or `ViewPager`. Bucket is built by the Manager from the View it manages. And it
has the responsibility to tell Manager about UI updates, as well as to select which Video to play
from all Videos it *knows* about. The View is called the _root of a Bucket_. You can setup any View
to be the root of a Bucket. A simple FrameLayout can be the root for a Bucket of all the Renderers
it contains.

Most of the time you want to use the most suitable root for your use case. For example, in your
screen you have a RecyclerView whose each child is a FrameLayout with a PlayerView in it. Either the
RecyclerView or each of its FrameLayout children can be the root of a Bucket, but if you want to
manage all the Videos at once, you need to use build the Bucket from the RecyclerView. **Kohii** has
built-in factory methods to build Bucket for `RecyclerView`, `NestedScrollView`, `ViewPager`
, `ViewPager2` and the general `ViewGroup`. You can have many Bucket in a single screen to enable
complicated behavior. For example: when a RecyclerView with many Videos is nested in another
RecyclerView, you can build the Bucket for both the RecyclerViews to control the Videos of the
nested one.

**Manager** is a component designed to manage a `Fragment` or an `Activity` which contains *big
View*s using Buckets. Manager creates and manages Buckets for Views on demand. You only need to
create Bucket for the View whose Video need to be controlled. So in our scenario above, Fragment B
only need to register the `RecyclerView` to **Kohii**, the Manager will then acknowledge
this `RecyclerView` and create a Bucket for it. Manager also manages all **Playbacks** in all
Buckets, and closely communicates with the **Group** about any UI change.

**Group** is as important as an Activity in Android framework. It contains many **Managers** just
like an `Activity` can contain many `Fragments`, and it listens to **Managers** request to refresh
the overall state. Because we will only allow a small number of videos from one *Bucket* of one *
Manager* at a time to be playing, *Group* exists to take care of playback state of all *Managers*,
and will carefully update the whole screen (i.e the `Activity`) accordingly.

## Master, Engine

**Master** is the component that controls everything used by the library. In short term, it is the
brain of **Kohii**. There will be only one Master instance exists at one time. It controls the
Playable's lifecycle, including destroying them when they are not needed anymore. It manages all the
Groups of an Application, dispatch the playback events and so on. The client should never need to
access the Master directly.

**Engine** is a component that connects the client with the Master. An Engine has 3 important
responsibilities: to initialize other important components like Managers, Groups, to help the client
to bind the Video resource to a Container, and to build Playable for the Video. Most of the time,
developer only needs to work with the Engine. You can also build custom Engine for your need.
