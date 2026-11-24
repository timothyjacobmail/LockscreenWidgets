package tk.zwander.lockscreenwidgets.ambient

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tk.zwander.common.activities.BaseActivity
import tk.zwander.common.compose.util.widgetViewCacheRegistry
import tk.zwander.common.host.widgetHostCompat
import tk.zwander.common.util.RemoteViewsLayoutInflaterContext
import tk.zwander.common.util.appWidgetManager
import tk.zwander.common.util.keyguardManager
import tk.zwander.common.util.logUtils
import tk.zwander.common.util.setThemedContent
import tk.zwander.lockscreenwidgets.R

internal class AmbientDisplayActivity : BaseActivity() {
    private data class BadgeResourceIds(val settings: Int, val status: Int, val rows: Int, val label: Int, val count: Int)

    private var controller: AmbientDisplayController? = null
    private var lastBadgeCounts: Map<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activeController = AmbientDisplayController.current
        val widget = activeController?.attach(this)
        if (widget == null) {
            finish()
            return
        }
        controller = activeController
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !keyguardManager.isKeyguardLocked) {
            finish()
            return
        }
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 0.03f }
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        if (widget.isBadge) {
            setThemedContent { DisplayBadgeWidget(widget) }
        } else {
            val hostView = try {
                widgetViewCacheRegistry.getOrCreateView(this, widget.id, widget.info)
            } catch (e: RuntimeException) {
                logUtils.normalLog("Unable to show the dim-screen widget", e)
                finish()
                return
            }
            hostView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            hostView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            hostView.isFocusable = false
            setThemedContent { DisplaySelectedWidget(widget, hostView) }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // The widget is a read-only copy. Touches return to the real lock screen.
        if (event.actionMasked == MotionEvent.ACTION_UP) finish()
        return true
    }

    override fun onStop() {
        super.onStop()
        controller?.activityStopped(this)
        controller = null
        finish()
    }

    override fun onDestroy() {
        controller?.activityStopped(this)
        super.onDestroy()
    }

    @Composable
    private fun DisplayBadgeWidget(widget: AmbientWidget) {
        val host = widgetHostCompat
        var views by remember(widget.id) { mutableStateOf(host.cachedRemoteViews[widget.id]) }
        var position by remember { mutableIntStateOf(0) }
        LaunchedEffect(widget.id) {
            launch {
                host.remoteViewsUpdates.collect { id ->
                    if (id == widget.id) views = host.cachedRemoteViews[id]
                }
            }
            while (isActive) {
                delay(60_000)
                position = (position + 1) % 9
            }
        }

        val options = remember(widget.id) { appWidgetManager.getAppWidgetOptions(widget.id) }
        val providerResources = remember(widget.id) {
            packageManager.getResourcesForApplication(widget.info.provider.packageName)
        }
        val badgeResourceIds = remember(providerResources) {
            fun id(name: String) = providerResources.getIdentifier(name, "id", widget.info.provider.packageName)
            BadgeResourceIds(id("badge_settings"), id("badge_status"), id("badge_rows"), id("badge_label"), id("badge_count"))
        }
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            .takeIf { it > 0 } ?: widget.info.minWidth
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            .takeIf { it > 0 } ?: widget.info.minHeight
        val dismissLabel = stringResource(R.string.ambient_return_to_lock_screen)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.Black)
                .semantics { onClick(dismissLabel) { finish(); true } }
                .safeDrawingPadding().padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val snapshot = views
            if (snapshot != null) {
                Box(
                    Modifier.size(width.dp.coerceIn(1.dp, maxWidth), height.dp.coerceIn(1.dp, maxHeight))
                        .offset(((position % 3 - 1) * 8).dp, ((position / 3 - 1) * 8).dp),
                ) {
                    AndroidView(
                        factory = { context ->
                            // Keep the original host view in its frame and reuse its RemoteViews updates.
                            AppWidgetHostView(RemoteViewsLayoutInflaterContext(context.applicationContext, widget.id)).apply {
                                setAppWidget(widget.id, widget.info)
                                setPadding(0, 0, 0, 0)
                                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            view.updateAppWidget(snapshot)
                            view.findViewById<View>(badgeResourceIds.settings)?.visibility = View.GONE
                            view.findViewById<View>(badgeResourceIds.status)?.visibility = View.GONE
                            view.findViewById<LinearLayout>(badgeResourceIds.rows)?.apply {
                                val counts = mutableMapOf<String, Int>()
                                updateLayoutParams<FrameLayout.LayoutParams> {
                                    gravity = Gravity.CENTER
                                    marginEnd = 0
                                }
                                children.filterIsInstance<LinearLayout>().forEach { row ->
                                    row.gravity = Gravity.CENTER_HORIZONTAL
                                    row.children.forEach { item ->
                                        val label = item.findViewById<TextView>(badgeResourceIds.label)
                                        val badge = item.findViewById<TextView>(badgeResourceIds.count)
                                        val count = badge?.text?.toString()?.trimEnd('+')?.toIntOrNull()
                                        if (label != null && count != null) counts[label.text.toString()] = count
                                        label?.visibility = View.GONE
                                    }
                                }
                                val previous = lastBadgeCounts
                                lastBadgeCounts = counts
                                if (previous != null && counts.any { it.value > (previous[it.key] ?: 0) }) {
                                    controller?.onNewNotification()
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun DisplaySelectedWidget(widget: AmbientWidget, hostView: AppWidgetHostView) {
        val density = LocalDensity.current
        var position by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(60_000)
                position = (position + 1) % 9
            }
        }
        val dismissLabel = stringResource(R.string.ambient_return_to_lock_screen)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.Black)
                .semantics { onClick(dismissLabel) { finish(); true } }
                .safeDrawingPadding().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val width = with(density) { widget.info.minWidth.toDp() }.coerceIn(1.dp, maxWidth)
            val height = with(density) { widget.info.minHeight.toDp() }.coerceIn(1.dp, maxHeight)
            LaunchedEffect(hostView, width, height) {
                val options = appWidgetManager.getAppWidgetOptions(widget.id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    hostView.updateAppWidgetSize(options, listOf(SizeF(width.value, height.value)))
                } else {
                    @Suppress("DEPRECATION")
                    hostView.updateAppWidgetSize(
                        options, width.value.toInt(), height.value.toInt(), width.value.toInt(), height.value.toInt(),
                    )
                }
            }
            AndroidView(
                factory = { hostView },
                modifier = Modifier
                    .offset(((position % 3 - 1) * 8).dp, ((position / 3 - 1) * 8).dp)
                    .size(width, height),
            )
        }
    }
}
