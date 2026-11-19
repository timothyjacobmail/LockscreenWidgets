package tk.zwander.lockscreenwidgets.ambient

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientQuietHoursTest {
    @Test fun overnightQuietHoursIncludeMidnightAndEndAtFive() {
        assertFalse(isInQuietHours(21 * 60 + 59, DEFAULT_QUIET_START, DEFAULT_QUIET_END))
        assertTrue(isInQuietHours(22 * 60, DEFAULT_QUIET_START, DEFAULT_QUIET_END))
        assertTrue(isInQuietHours(0, DEFAULT_QUIET_START, DEFAULT_QUIET_END))
        assertTrue(isInQuietHours(4 * 60 + 59, DEFAULT_QUIET_START, DEFAULT_QUIET_END))
        assertFalse(isInQuietHours(5 * 60, DEFAULT_QUIET_START, DEFAULT_QUIET_END))
    }

    @Test fun daytimeAndAllDayQuietHoursUseTheSameBoundaries() {
        assertTrue(isInQuietHours(8 * 60, 8 * 60, 12 * 60))
        assertFalse(isInQuietHours(12 * 60, 8 * 60, 12 * 60))
        assertTrue(isInQuietHours(0, 8 * 60, 8 * 60))
    }
}
