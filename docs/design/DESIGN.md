## Roadmap

- [x] Basic binding
  - [x] Simple syntax to start binding a Video to a ViewGroup (PlayerView or a ViewGroup that will contain a PlayerView), using Url, Uri or specific Media object.
  - [x] Automatically update the playbacks in RecyclerView, NestedScrollView or general ViewGroup.
  - [x] Automatically update the playbacks in ViewPager, ViewPager2.

- [x] Handle configuration change
  - [x] Retain resource on configuration changes.

- [ ] Manual control over Playbacks
  - [ ] Allow client to have full control over Playbacks: client can start/pause any Playback without the control of system/Kohii.
  - [ ] Allow client to have half-manual control over Playbacks: client can start/pause any Playback on demand, but Kohii can also start/pause the Playback regarding some conditions.

- [ ] Background Playback support
  - [ ] Allow configuration to enable/disable background Playback. The config would allow to setup: flag to turn the feature on/off, necessary information for the foreground notification.
  - [ ] Allow the Playback to keep playing after closing the App.
  - [ ] Allow the Playback to keep playing when: the Playback is deselected from the Manager, but no newer Playback is selected.

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