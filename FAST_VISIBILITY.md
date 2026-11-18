# Faster widget visibility

Version `4.7.4-fast.2` keeps each frame window, Compose hierarchy, and hosted widgets
attached after their first appearance. Hiding sets window alpha to zero, disables
input, hides accessibility descendants, clears screen-on flags, and stops the frame
lifecycle/Compose animation clock. Waking restores the same window without another
surface-add debounce or rebuilding the widget layout. Disabling frames, changing
displays, or destroying the service releases the windows.

Frame and drawer show/hide animations default to off. Explicit saved choices are
preserved; the animation switches remain available in settings. Drawer handle taps
now open the drawer by default, alongside the existing swipe gesture.

Drawer and handle transitions are serialized so overlapping show/close/screen events
cannot cancel each other's fade and then detach a newly shown window. Failed window
adds reset the transition state, and close requests restore an eligible handle even
when the drawer was already closed. Visibility checks and Tasker queries distinguish
an open drawer from its handle, and visible frames from retained hidden frames.

Accessibility processing does not wait for frame transitions. Cold frame attachment
retains the upstream 50 ms safety delay; repeated show requests do not restart it.
A hide request cancels a pending appearance. Retained windows skip this delay.

This uses more retained memory. It does not hold a CPU wake lock or keep the display
on while hidden. Android may still reclaim the app or its surfaces, and lock-screen
state detection still controls when a frame can appear. This is not an AOD feature
or a guarantee of zero end-to-end latency.

## Building and checking

Follow the SDK/hidden-API instructions in README.md. Build and run the regression suite:

```text
gradlew :app:assembleDebug :app:testDebugUnitTest
```

`FrameVisibilityControllerTest` covers event bursts, cancellation, in-flight
transitions, and repeated retained-window wake cycles without reattachment or delay.
On-device checks cover hidden-window input flags, reuse of the same windows across
screen-off/wake, drawer button/tap/swipe opening, closing, and handle recovery.

A locally signed APK cannot update an upstream APK signed by Zachary Wander.
Back up settings before replacing an upstream installation. Later builds using the
same local signing key can update this fork in place with `adb install -r`.
