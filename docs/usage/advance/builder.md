## Using Builder

**Kohii** instance can be constructed using `Builder`. By default, calling `Kohii[context]` will create or reuse an instance with default implementation. For advance users, it is more flexible to be able to customize this. **Kohii** provides `Builder` to make this happen:

```Kotlin tab=
val playableCreator = MyCustomPlayableCreator()
val builder = Kohii.Builder(context)
    .setPlayableCreator(playableCreator)
val kohii = builder.build()
```

```Java tab=
PlayableCreator<PlayerView> playableCreator = new MyCustomPlayableCreator();
Kohii.Builder builder = new Kohii.Builder(context)
    .setPlayableCreator(playableCreator);
Kohii kohii = builder.build();
```

If you still want to use the default `PlayerViewPlayableCreator`, it can be constructed by its own Builder too, which will requires a `BridgeCreatorFactory` which is of type `Context.() -> BridgeCreator<PlayerView>`:

```Kotlin tab=
val playableCreator: PlayableCreator<PlayerView> =
  PlayerViewPlayableCreator.Builder(this)
    .setBridgeCreatorFactory(factory).build()
```

Please take a look at the source for all available builder parameters.
