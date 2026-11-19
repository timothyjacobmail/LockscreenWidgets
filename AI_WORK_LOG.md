# AI work log

## 2026-11-19

- The account fork and local `faster-widget-visibility` branch matched at `b549ad74` before the pre-existing widget-host changes. The local `master` tracks the upstream project. Use the account fork for local changes.
- Phone package `tk.zwander.lockscreenwidgets` was installed with version code 228.
- Pushed the working-tree changes in `d8bba22e` on `faster-widget-visibility`. The debug build and JVM unit-test task passed.
- Device verification remains important for eligible widget taps over a locked keyguard, an unrelated provider's taps, and cleanup of the drawer and frame state during a tap.
