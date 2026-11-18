package tk.zwander.widgetdrawer.util

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import tk.zwander.common.util.LSDisplay
import tk.zwander.common.util.lsDisplayManager
import tk.zwander.common.util.statusBarHeight
import kotlin.math.roundToInt

internal object HandlePosition {
    fun maxOffset(availableHeight: Int, handleHeight: Int): Int =
        (availableHeight - handleHeight).coerceAtLeast(0)

    fun fromPercent(percent: Int, maxOffset: Int): Int =
        (percent.coerceIn(0, 100) / 100.0 * maxOffset.coerceAtLeast(0)).roundToInt()

    fun toPercent(offset: Int, maxOffset: Int): Int =
        if (maxOffset <= 0) 0 else
            (offset.coerceIn(0, maxOffset) * 100.0 / maxOffset).roundToInt()
}

internal fun Context.drawerHandleMaxY(display: LSDisplay, heightDp: Int): Int {
    val manager = lsDisplayManager.displayAndWmCache.value[display.uniqueIdCompat]?.windowManager
    val availableHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) {
        val metrics = manager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )
        metrics.bounds.height() - insets.top - insets.bottom
    } else {
        val navId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navHeight = if (navId != 0) resources.getDimensionPixelSize(navId) else 0
        display.rotatedRealSize.y - statusBarHeight - navHeight
    }
    return HandlePosition.maxOffset(availableHeight, display.dpToPx(heightDp))
}
