package tk.zwander.lockscreenwidgets.ambient

import kotlinx.coroutines.flow.MutableStateFlow

internal object AmbientDisplayState {
    val showing = MutableStateFlow(false)
    const val LOCK_SCREEN_NOTIFICATIONS = "lock_screen_show_notifications"
}

internal const val AMBIENT_ENABLED = "ambient_notifications_enabled"
internal const val AMBIENT_QUIET_HOURS = "ambient_notifications_quiet_hours"
internal const val AMBIENT_QUIET_START = "ambient_notifications_quiet_start"
internal const val AMBIENT_QUIET_END = "ambient_notifications_quiet_end"
internal const val DEFAULT_QUIET_START = 22 * 60
internal const val DEFAULT_QUIET_END = 5 * 60

internal fun isInQuietHours(now: Int, start: Int, end: Int): Boolean = when {
    start < end -> now in start until end
    start > end -> now >= start || now < end
    else -> true
}
