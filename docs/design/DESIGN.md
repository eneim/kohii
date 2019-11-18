## Roadmap

- [x] Basic binding
  - [x] Simple syntax to start binding a Video to a ViewGroup (PlayerView or a ViewGroup that will contain a PlayerView), using Url, Uri or specific Media object.
  - [x] Automatically update the playbacks in RecyclerView, NestedScrollView or general ViewGroup.
  - [x] Automatically update the playbacks in ViewPager, ViewPager2.

- [ ] Callbacks
  - [x] Callbacks for **Kohii** components status.
  - [ ] Callbacks for playback status.
  - [ ] (Nice to have) *Opinionated callbacks* that tell client when it *should show/hide thumbnail*.

- [x] Handle configuration change
  - [x] Retain playback on configuration changes.

- [ ] Manual control over Playbacks
  - [x] Allow client to have full control over Playbacks: client can start/pause any Playback without the control of system/Kohii.
  - [x] Allow client to have half-manual control over Playbacks: client can start/pause any Playback on demand, but Kohii can also start/pause the Playback regarding some conditions.
  - [ ] Global control: simple method to pause/resume everything. The resume behavior follow each Playback's config.

- [ ] Volume control
  - [ ] Sophisticated Volume control at many levels: Kohii scope (global), Manager scope, Host scope, Playback scope.

- [ ] ExoPlayer related implementations
  - [x] First-class support for playback using ExoPlayer/PlayerView.
  - [x] Cached playback using ExoPlayer cache mechanism.
  - [ ] Offline/downloaded playback using ExoPlayer download mechanism.

- [ ] Extensible architecture
  - [x] Base abstraction.
  - [x] Default implementation for ExoPlayer.
  - [x] (Nice to have) Experiment implementation for YouTube Videos using YouTube Player API.
  - [x] (Nice to have) Experiment implementation for YouTube Videos using OSS playback library.
  - [ ] (Nice to have) Experiment implementation for AndroidX Media 2.
  - [ ] (Nice to have) Experiment implementation for Platform MediaPlayer/VideoView.

- [ ] Others
  - [ ] Flag/Callback to enable/disable automatic playback. Useful to trigger the feature on demand (eg: due to Network quality changes).
  - [x] ``MemoryMode`` setting, allows client to control how Videos will be kept when it is not playing. For example in HIGH MemoryMode, many Videos can keep resource at the same time, so the playback will be really smooth, but in LOW MemoryMode, any paused Video will be released to save resource.
  - [ ] Ads support with Mopub, IMA.

- [ ] Demos
  - [x] Basic RecyclerView sample, no nesting.
  - [x] Basic NestedScrollView sample, no nesting.
  - [x] RecyclerView with NestedScrollView nested in a ViewHolder.
  - [ ] RecyclerView with RecyclerView nested in a ViewHolder.
  - [x] NestedScrollView with a RecyclerView nested inside.
  - [x] RecyclerView with user interaction (eg: Click).
  - [x] NestedScrollView with user interaction (eg: Click).
  - [x] ViewPager where pages are PlayerViews or FrameLayouts.
  - [ ] ViewPager where pages are RecyclerViews with Videos.
  - [x] ViewPager where pages are Fragments contain PlayerViews or FrameLayout.
  - [ ] ViewPager where pages are Fragments contain RecyclerView with Videos.
  - [x] ViewPager2 where pages are PlayerViews or FrameLayouts.
  - [ ] ViewPager2 where pages are RecyclerViews with Videos.
  - [x] ViewPager2 where pages are Fragments contain PlayerViews or FrameLayout.
  - [ ] ViewPager2 where pages are Fragments contain RecyclerView with Videos.
  - [ ] Multiple Fragments where each contains RecyclerView with Videos.
  - [x] Sample to mimic Facebook.
  
- [ ] Background Playback support
  - [ ] Allow configuration to enable/disable background Playback. The config would allow to setup: flag to turn the feature on/off, necessary information for the foreground notification (eg Bitmap for the Notification large image).
  - [ ] Allow the Playback to keep playing after closing the App on-demand.
  - [ ] Allow the Playback to keep playing when: the Playback is deselected from the Manager, but no newer Playback is selected.

- [ ] Others
  - [ ] Store PlaybackInfo to pages + local DB to optimize/reduce in-memory cache.

## Resource creation

## Binding Playable to Container (ViewGroup)

- The actual binding only happens once the Container is attached to Window. The result will be a Playback that has reference to the Container.

- After being bound, the Container might be detached/re-attached without the need to remove the Playback.

- States of a Playback:
  - 'ADDED': the binding is established.
  - 'DETACHED': the Container is detached from Window.
  - 'ATTACHED, INACTIVE': the Container is attached to Window, but its visible area is < 0.
  - 'ATTACHED, ACTIVE': the Container is attached to Window, but its visible area is >= 0.
  - 'REMOVED': the binding is destroyed.

- Lifecycle of a Playback: a Playback lives through the 'ADDED' state and 'REMOVED' state. A 'REMOVED' Playback must also be 'INACTIVE' and 'DETACHED'.
  - Lifecycle of a Playback relies on its Container (ViewGroup)'s lifecycle, the Host that contains that Container and the Manager that contains the Host. 

- Binding scenarios and the behavior:
  - Bind a Playable to a fresh Container:
    - Establish new binding (new Playback will be created and added to the Manager). 
    - Any old Playback referenced by Playable must be removed.
  - Bind a Playable to a Container that was bound to different Playable: 
    - Establish new binding (new Playback will be created and added to the Manager).
    - Any old Playback referenced by Playable must be removed.
    - Any Playback reference to the same Container must be removed.
  - Bind a Playable to a Container that was bond to the same Playable: no action required. 

- Manager lifecycle and its affects:
  - A Manager lifecycle is bound to its 'origin's lifecycle'. The origin can be ComponentActivity or Fragment.
  - ON_STOP event: all Playbacks must be transferred to up-to 'INACTIVE' state. 
  - ON_DESTROY event: all Playbacks must be transferred to up-to 'REMOVED' state.
  - ON_START event: refresh the status of Playbacks to its latest state. 