# Faster widget visibility

This fork reduces the delay when widget frames appear and disappear.

- Accessibility processing no longer waits for a frame's fade animation. It can continue
  checking the lock screen, notification shade, and other visibility conditions.
- Repeated show requests share one 50 ms safety interval instead of restarting that
  interval on every event. A hide request interrupts a pending show.
- Attached frames skip the show delay. Add/remove animations run serially, and the
  latest visibility state is checked before each transition.
- The default frame fade duration is 120 ms instead of 300 ms. Existing explicitly
  saved durations remain unchanged; use Frame Settings > Animation Duration to adjust
  them. Drawer animations are unchanged.

The upstream surface-creation delay, draw synchronization, and window-removal safeguards
remain in place. A hide requested during an animation runs after that animation finishes.
These changes do not guarantee a particular end-to-end latency on every Android device.

## Building and checking

Follow the SDK/hidden-API instructions in README.md. Build and run the regression suite:

```text
gradlew :app:assembleDebug :app:testDebugUnitTest
```

`FrameVisibilityControllerTest` covers event bursts, canceling a pending show, updating an
attached frame, and show/hide reversals during animations using virtual time.

The version name is `4.7.4-fast.1`. A locally signed APK cannot update an upstream APK
signed by Zachary Wander. Back up the app's frame/drawer settings before replacing an
upstream installation. Widget bindings and Android permissions may need to be restored.

