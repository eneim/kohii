## Design purpose

#### General concepts

- Playback: the concept of a media content is being rendered and shown to user in an application. Here we will only discuss video playback.
- Playback Continuity: the concept by that a playback is fully or partly kept playing continuously across various lifecycle states or lifecycle events of the application component that renders the playback. For example: during a device rotation, while a playback's video part is re-rendered, its audio part is kept playing without any interruption.

#### What kind of App should use Kohii

- 1. App wants to show list of items that contains Videos. Videos are play/paused automatically without User interaction other than scrolling the list. At most one Video is being played at a time. The list is implemented using RecyclerView, video playback is implemented using ExoPlayer.
- 2. Same as 1, but the list is implemented using NestedScrollView.
- 3. Same as 1, but instead of list, the App shows content in pages. The pages container is implemented using ViewPager or ViewPager2. Each page is a Fragment or a ViewGroup.
- 1 or 2 or 3, and the "playback continuity" is kept across configuration change, or lifecycle change (bringing a Video from one Fragment to another, or from one Activity to another).
- 1 or 2 or 3, and only small number of playback output is in used. In other word, the playback output Surface is reused for infinite number of Videos.


#### What Kohii library can do

- Manage hosts (mostly ViewGroups like RecyclerView, NestedScrollView, ViewPager, etc) whose scroll should be observed.