## Terms of objects in Kohii library:

- **Kohii**: name of the library, also the main class of the library. It follows *Singleton* pattern to manage its instance. Only one instance of **Kohii** is created and managed for one **Application**.

- **Manager**

- **Playable**

- **Target**

- **Playback**

## Creating Kohii instance:

- Create one **Kohii** instance by calling ``Kohii[activity]`` or ``Kohii[fragment]``. Currently no other types of parameter is allowed.

## Creating a Playable:

- Calling ``Kohii[context].setUp(myVideo)`` will init **Kohii** on demand, and will create a new ``Playable.Builder`` instance.

- This **Builder** is different to general Builder in common patterns, in that calling ``build()`` methods (in this case: ``builder.asPlayable()``) will not always create new ``Playable`` instance. Its fields value are used for building the Playback resource instead.

- ``Playable`` instances are cached by **Kohii** if a valid *tag* is provided to the ``Builder``. Calling ``builder.asPlayable()`` will internally request **Kohii** to find one from cache, or create new one.

## When a playable is considered 'unavailable':

- Created, but not bound to any target.
- Bound to a target, but its target is inactive, and it has not rebound to any other active target.
- Bound to a target, and regardless of the status of its target, its Manager is stopped/inactive.

## When a target is considered 'unavailable':

- Target is a View, and it is not attached to the Window, or it has been detached from the Window.
- Regardless of the target status, its Manager is stopped/inactive.

## [20180905]

- A Playback should be configurable even after created. This way, user can change the behaviour on demand. All the changes will be reflected via Playable and Bridge instance.