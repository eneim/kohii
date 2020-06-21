## Using MemoryMode to improve UX

Your screen may contain many Videos at a time, and preload Videos forward so they can start as soon as possible is a legit requirement. In practice, preloading Videos consumes a lot of system resource like memory, network and power. To address this need in proper way, **Kohii** provides a special control flag called [MemoryMode](../api/kohii-core/kohii.v1.core/-memory-mode/). The idea behinds `MemoryMode` is as below:

- The idea is to allow **Kohii** to _preload around Video of interest[^1]_. When _the Video of interest_ is playing, at the same time **Kohii** will prepare the closet Videos _around_ it: the first on top, the first below, the first to the left and/or the first to the right, and the second closet Videos, etc ...

- To control the maxinum number of Videos to prepare, **Kohiii** needs client to explixitly define this (with a default behavior to be automatic).

`MemoryMode` is the information client uses to define this behavior. To use it, you need to update the code as below:

```diff
- kohii.register(this)
+ kohii.register(this, MemoryMode.BALANCED)
```

While allowing client to define this behavior, **Kohii** will strictly observe system memory status, and will override that behavior on demand to prevent your app from behaving unexpectedly.

[^1]: The Video that is selected to play
