## Reuse PlayerView instance for multiple Videos

Until now, the setup code is always `kohii.setUp(videoUrl).bind(playerView)` which may let you think that you will need to bind the Video to a `PlayerView` instance.

In **Kohii**, target of the method `bind` is called [`container`](/usage/glossary/#renderer-and-container). While `PlayerView` is the place where Video content is rendered (and therefore it is called [`renderer`](/usage/glossary/#renderer-and-container), it can also be a `container` which _contains itself_).

You can bind the Video to any `ViewGroup` as container, as long as either it is a `renderer` itself, or it has no children so that it _can contain_ a `renderer` later. When you bind to a _non-renderer_ `container`, for example an empty `FrameLayout`, **Kohii** will automatically prepare and add the `PlayerView` instance to that `FrameLayout` dynamically. At the same time, the unnecessary `PlayerView` instance will be removed from `container` and put back to a `Pool`. This way, only a few `PlayerView` instances will be created and reused for as many container/Videos as possible.

By default, **Kohii** has its own logic for creating and recycling `PlayerView`, but developers can build their own by extending [**Engine**](/api/kohii-core/kohii.v1.core/-engine) - another important component of **Kohii**. Extending **Engine** and building custom playback logic will be discussed in topics for developers.
