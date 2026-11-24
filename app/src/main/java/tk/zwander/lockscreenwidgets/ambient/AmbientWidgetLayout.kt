package tk.zwander.lockscreenwidgets.ambient

import android.appwidget.AppWidgetHostView
import android.os.Build
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import tk.zwander.common.compose.components.PlacementEdges
import tk.zwander.common.compose.components.PlacementMoveControls
import tk.zwander.common.compose.components.Side
import tk.zwander.common.compose.util.widgetViewCacheRegistry
import tk.zwander.common.util.appWidgetManager
import tk.zwander.common.util.prefManager
import tk.zwander.lockscreenwidgets.R

@Composable
internal fun AmbientWidgetLayout(widgets: List<AmbientWidget>, editing: Boolean, close: () -> Unit) {
    val context = LocalContext.current
    val placements = remember {
        mutableStateMapOf(*context.prefManager.ambientWidgets.map { it.id to requireNotNull(it.drawerPlacement) }.toTypedArray())
    }
    val views = remember(widgets) {
        widgets.mapNotNull { widget ->
            runCatching {
                context.widgetViewCacheRegistry.getOrCreateView(context, widget.id, widget.info).apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    isFocusable = false
                }
            }.getOrNull()?.let { widget.id to it }
        }.toMap()
    }
    var selected by remember { mutableIntStateOf(widgets.first().id) }
    var shift by remember { mutableIntStateOf(0) }
    LaunchedEffect(editing) {
        if (!editing) while (true) {
            delay(60_000)
            shift = (shift + 1) % 9
        }
    }
    fun save() {
        val prefs = context.prefManager
        prefs.ambientWidgets = prefs.ambientWidgets.map {
            it.copy(drawerPlacement = placements[it.id] ?: it.drawerPlacement)
        }.toSet()
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(12.dp).clipToBounds(),
        contentAlignment = AbsoluteAlignment.TopLeft,
    ) {
        val width = maxWidth
        val height = maxHeight
        val widthPx = constraints.maxWidth.coerceAtLeast(1)
        val heightPx = constraints.maxHeight.coerceAtLeast(1)
        val minW = (64f / width.value.coerceAtLeast(1f)).coerceAtMost(1f)
        val minH = (64f / height.value.coerceAtLeast(1f)).coerceAtMost(1f)
        fun gesture(id: Int, side: Side? = null) = Modifier.pointerInput(id, side, widthPx, heightPx) {
            detectDragGestures(onDragEnd = { save() }, onDragCancel = { save() }) { change, delta ->
                change.consume()
                val bounds = placements.getValue(id)
                val dx = delta.x / widthPx
                val dy = delta.y / heightPx
                placements[id] = when (side) {
                    Side.LEFT -> bounds.resizeLeft(dx, minW)
                    Side.TOP -> bounds.resizeTop(dy, minH)
                    Side.RIGHT -> bounds.resized(dx, 0f, minW, 0f)
                    Side.BOTTOM -> bounds.resized(0f, dy, 0f, minH)
                    null -> bounds.moved(dx, dy)
                }
            }
        }
        widgets.forEach { widget ->
            key(widget.id) {
                val bounds = placements.getValue(widget.id)
                val host = views[widget.id]
                val active = editing && widget.id == selected
                val widgetWidth = width * bounds.w
                val widgetHeight = height * bounds.h
                val shiftX = if (shift == 0) 0.dp else ((shift % 3 - 1) * 4).dp
                val shiftY = if (shift == 0) 0.dp else ((shift / 3 - 1) * 4).dp
                Box(
                    Modifier.zIndex(if (active) 1f else 0f)
                        .absoluteOffset(
                            (width * bounds.x + shiftX).coerceIn(0.dp, width - widgetWidth),
                            (height * bounds.y + shiftY).coerceIn(0.dp, height - widgetHeight),
                        )
                        .size(widgetWidth, widgetHeight),
                ) {
                    if (host != null) {
                        LaunchedEffect(host, widgetWidth, widgetHeight) {
                            if (editing) delay(200)
                            updateAmbientWidgetSize(host, widgetWidth.value, widgetHeight.value)
                        }
                        AndroidView(factory = { host }, modifier = Modifier.fillMaxSize())
                    }
                    if (editing) {
                        Box(Modifier.fillMaxSize().border(if (active) 3.dp else 1.dp, Color.White)
                            .clickable { selected = widget.id })
                        if (active) PlacementEdges { side -> gesture(widget.id, side) }
                    }
                }
            }
        }
        if (editing) {
            val bounds = placements.getValue(selected)
            val toolbar = if (bounds.y + bounds.h / 2 > .5f) Alignment.TopCenter else Alignment.BottomCenter
            Surface(Modifier.align(toolbar).zIndex(2f), color = Color(0xFF333333)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val id = selected
                    PlacementMoveControls(
                        centerVertically = {
                            val bounds = placements.getValue(id)
                            placements[id] = bounds.copy(y = (1f - bounds.h) / 2)
                            save()
                        },
                        centerHorizontally = {
                            val bounds = placements.getValue(id)
                            placements[id] = bounds.copy(x = (1f - bounds.w) / 2)
                            save()
                        },
                        moveModifier = gesture(id),
                    )
                    Row {
                        TextButton(onClick = {
                            selected = widgets[(widgets.indexOfFirst { it.id == id } + 1) % widgets.size].id
                        }) { Text(stringResource(R.string.drawer_cycle_widget)) }
                        TextButton(onClick = close) { Text(stringResource(R.string.drawer_done)) }
                    }
                }
            }
        }
    }
}

private fun updateAmbientWidgetSize(host: AppWidgetHostView, width: Float, height: Float) {
    val options = host.context.appWidgetManager.getAppWidgetOptions(host.appWidgetId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        host.updateAppWidgetSize(options, listOf(SizeF(width, height)))
    } else {
        @Suppress("DEPRECATION")
        host.updateAppWidgetSize(options, width.toInt(), height.toInt(), width.toInt(), height.toInt())
    }
}
