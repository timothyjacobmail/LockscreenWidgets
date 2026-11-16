package tk.zwander.lockscreenwidgets.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrameVisibilityControllerTest {
    private class Frame(scope: TestScope, animationMillis: Long = 0) {
        var wanted = true
        var attached = false
        var shows = 0
        var hides = 0
        var animating = false
        var settled = 0
        val controller = FrameVisibilityController(
            scope = scope.backgroundScope,
            showDelayMillis = 50,
            shouldShow = { wanted },
            isAttached = { attached },
            show = {
                assertFalse("Window transitions must not overlap", animating)
                if (!attached) {
                    animating = true
                    shows++
                    attached = true
                    delay(animationMillis)
                    animating = false
                }
            },
            hide = {
                assertFalse("Window transitions must not overlap", animating)
                if (attached) {
                    animating = true
                    hides++
                    delay(animationMillis)
                    attached = false
                    animating = false
                }
            },
            onSettled = { settled++ },
        )
    }

    @Test
    fun repeatedShowEventsDoNotPostponeAppearance() = runTest {
        val frame = Frame(this)
        frame.controller.requestUpdate()
        runCurrent()
        repeat(4) {
            advanceTimeBy(10)
            frame.controller.requestUpdate()
            runCurrent()
        }
        assertEquals(0, frame.shows)
        advanceTimeBy(10)
        runCurrent()
        assertEquals(1, frame.shows)
    }

    @Test
    fun hideDuringSafetyDelayPreventsStaleShow() = runTest {
        val frame = Frame(this)
        frame.controller.requestUpdate()
        runCurrent()
        advanceTimeBy(10)
        frame.wanted = false
        frame.controller.requestUpdate()
        runCurrent()
        assertEquals(1, frame.settled)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(0, frame.shows)
        assertFalse(frame.attached)
    }

    @Test
    fun attachedFrameUpdatesWithoutAnotherSafetyDelay() = runTest {
        val frame = Frame(this)
        frame.attached = true
        frame.controller.requestUpdate()
        runCurrent()
        assertEquals(1, frame.settled)
        assertEquals(0, frame.shows)
    }

    @Test
    fun idleAttachedFrameStartsHidingImmediately() = runTest {
        val frame = Frame(this, animationMillis = 120)
        frame.attached = true
        frame.wanted = false
        frame.controller.requestUpdate()
        runCurrent()
        assertEquals(1, frame.hides)
        assertTrue(frame.animating)
    }

    @Test
    fun destroyingScopeCancelsPendingAppearance() = runTest {
        val frame = Frame(this)
        frame.controller.requestUpdate()
        runCurrent()
        backgroundScope.cancel()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(0, frame.shows)
    }

    @Test
    fun hideIsProcessedAfterInFlightShowWithoutOverlappingAnimations() = runTest {
        val frame = Frame(this, animationMillis = 120)
        frame.controller.requestUpdate()
        advanceTimeBy(50)
        runCurrent()
        assertTrue(frame.animating)
        frame.wanted = false
        frame.controller.requestUpdate()
        runCurrent()
        assertEquals(0, frame.hides)
        advanceTimeBy(120)
        runCurrent()
        assertEquals(1, frame.hides)
        advanceTimeBy(120)
        runCurrent()
        assertFalse(frame.attached)
    }

    @Test
    fun showRequestedDuringRemovalIsNotLost() = runTest {
        val frame = Frame(this, animationMillis = 120)
        frame.attached = true
        frame.wanted = false
        frame.controller.requestUpdate()
        runCurrent()
        advanceTimeBy(10)
        frame.wanted = true
        frame.controller.requestUpdate()
        advanceTimeBy(110)
        runCurrent()
        assertFalse(frame.attached)
        advanceTimeBy(50)
        runCurrent()
        assertEquals(1, frame.shows)
        assertTrue(frame.attached)
        advanceTimeBy(120)
        runCurrent()
        assertFalse(frame.animating)
    }
}
