package tk.zwander.lockscreenwidgets.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Serializes window transitions without making accessibility processing wait for animations. */
internal class FrameVisibilityController(
    scope: CoroutineScope,
    showDelayMillis: Long,
    shouldShow: () -> Boolean,
    isAttached: () -> Boolean,
    show: suspend () -> Unit,
    hide: suspend () -> Unit,
    onSettled: suspend () -> Unit,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                if (shouldShow() && !isAttached()) {
                    // Keep the surface-creation safety delay, but don't restart it for every
                    // accessibility event. A hide request interrupts the wait immediately.
                    withTimeoutOrNull(showDelayMillis) {
                        while (shouldShow()) requests.receive()
                    }
                }

                // Recheck after waiting: the screen may have unlocked or powered off.
                // Never overlap add/remove animations or tear down a surface mid-animation.
                if (shouldShow()) show() else hide()
                onSettled()
            }
        }
    }

    fun requestUpdate() {
        requests.trySend(Unit)
    }
}
