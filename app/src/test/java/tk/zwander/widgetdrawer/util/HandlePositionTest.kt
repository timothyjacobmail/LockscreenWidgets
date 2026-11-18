package tk.zwander.widgetdrawer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HandlePositionTest {
    @Test
    fun positionsCoverTopMiddleAndBottomWithoutClippingHandle() {
        val maxY = HandlePosition.maxOffset(1489, 311)
        assertEquals(1178, maxY)
        assertEquals(0, HandlePosition.fromPercent(0, maxY))
        assertEquals(589, HandlePosition.fromPercent(50, maxY))
        assertEquals(1178, HandlePosition.fromPercent(100, maxY))
        for (percent in 0..100) {
            assertEquals(percent, HandlePosition.toPercent(HandlePosition.fromPercent(percent, maxY), maxY))
        }
    }

    @Test
    fun oversizedHandleAndRotationClampSavedOffsets() {
        assertEquals(0, HandlePosition.maxOffset(300, 500))
        assertEquals(0, HandlePosition.toPercent(1200, 0))
        assertEquals(100, HandlePosition.toPercent(1200, 300))
        assertEquals(0, HandlePosition.toPercent(-10, 300))
        assertEquals(300, HandlePosition.fromPercent(110, 300))
        assertEquals(0, HandlePosition.fromPercent(-10, 300))
    }
}
