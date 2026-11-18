package tk.zwander.widgetdrawer.compose

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tk.zwander.common.compose.components.PlacementEdges
import tk.zwander.common.compose.components.PlacementMoveControls
import tk.zwander.common.compose.components.Side
import tk.zwander.common.compose.AddItem
import tk.zwander.common.compose.WidgetItem
import tk.zwander.common.data.DrawerPlacement
import tk.zwander.common.data.WidgetData
import tk.zwander.common.util.globalState
import tk.zwander.common.util.set
import tk.zwander.common.util.remove
import tk.zwander.common.listeners.WidgetResizeListener
import tk.zwander.lockscreenwidgets.R
import tk.zwander.widgetdrawer.util.DrawerDelegate
import kotlin.math.roundToInt

@Composable
internal fun DrawerDelegate.DrawerViewModel.FreeDrawer(
    widgets: List<WidgetData>, update: (List<WidgetData>) -> Unit, columns: Int, rows: Int,
    locked: Boolean, add: () -> Unit, reconfigure: (Int, AppWidgetProviderInfo) -> Unit,
    changeIcon: (Int) -> Unit, modifier: Modifier = Modifier,
) {
    val editing by arrangingWidgets.collectAsState()
    val arranging = editing && !locked
    val editingId by currentEditingInterfaceId.collectAsState()
    LaunchedEffect(arranging, editingId) {
        globalState.handlingClick.remove(holderId)
        globalState.itemIsActive[holderId] = arranging || editingId != null
    }
    val placed = remember(widgets, columns, rows) {
        val used = widgets.mapNotNull { it.drawerPlacement }.toMutableList()
        widgets.map { item ->
            if (item.drawerPlacement != null) item else {
                val size = item.safeSize
                val bounds = DrawerPlacement.next(used,
                    (size.safeWidgetWidthSpan.toFloat() / columns).coerceIn(.05f, 1f),
                    (size.safeWidgetHeightSpan.coerceAtLeast(5).toFloat() / rows).coerceIn(.05f, 1f))
                used.add(bounds)
                item.copy(drawerPlacement = bounds)
            }
        }
    }
    LaunchedEffect(placed) { if (placed != widgets) update(placed) }
    val latest by rememberUpdatedState(placed)
    val save by rememberUpdatedState(update)
    var selected by remember { mutableStateOf<String?>(null) }
    val pages = placed.map { it.drawerPlacement!!.page }.distinct().sorted()
    var pageIndex by remember { mutableIntStateOf(0) }
    val index = pageIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    val items = placed.filter { it.drawerPlacement!!.page == pages.getOrNull(index) }
    Box(modifier) {
        if (pages.size > 1) {
            Surface(Modifier.align(Alignment.TopCenter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pageIndex = index - 1 }, enabled = index > 0) { Text("‹") }
                    Text("${index + 1}/${pages.size}")
                    TextButton(onClick = { pageIndex = index + 1 }, enabled = index < pages.lastIndex) { Text("›") }
                }
            }
        }
        if (arranging) {
            Surface(Modifier.align(Alignment.BottomCenter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { selected = items[(items.indexOfFirst { it.gridId == selected } + 1) % items.size].gridId }) {
                            Text(stringResource(R.string.drawer_cycle_widget))
                        }
                        if (items.any { it.gridId == selected }) TextButton(onClick = {
                            arrangingWidgets.value = false
                            currentEditingInterfaceId.value = selected
                        }) { Text(stringResource(R.string.settings)) }
                    }
                    TextButton(onClick = { arrangingWidgets.value = false }) { Text(stringResource(R.string.drawer_done)) }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = if (pages.size > 1) 48.dp else 0.dp, bottom = 50.dp)
            .clipToBounds(), contentAlignment = AbsoluteAlignment.TopLeft) {
            if (items.isEmpty()) AddItem(add, Modifier.fillMaxWidth().height(80.dp))
            val wide = constraints.maxWidth.coerceAtLeast(1)
            val tall = constraints.maxHeight.coerceAtLeast(1)
            val density = LocalDensity.current
            val minPx = with(density) { 48.dp.toPx() }
            items.forEach { item -> key(item.gridId) {
                val saved = item.drawerPlacement!!
                var draft by remember(saved) { mutableStateOf(saved) }
                fun commit() { save(latest.map { if (it.gridId == item.gridId) it.copy(drawerPlacement = draft) else it }) }
                fun gesture(resize: Boolean, side: Side? = null) = Modifier.pointerInput(saved, wide, tall, resize, side) {
                    detectDragGestures(onDragStart = { selected = item.gridId; if (resize) isResizingItem.value = true },
                        onDragCancel = { draft = saved; isResizingItem.value = false },
                        onDragEnd = { commit(); isResizingItem.value = false }) { change, delta ->
                        change.consume()
                        val dx = delta.x / wide
                        val dy = delta.y / tall
                        draft = when (side) {
                            Side.LEFT -> draft.resizeLeft(dx, minPx / wide)
                            Side.TOP -> draft.resizeTop(dy, minPx / tall)
                            Side.RIGHT -> draft.resized(dx, 0f, minPx / wide, 0f)
                            Side.BOTTOM -> draft.resized(0f, dy, 0f, minPx / tall)
                            null -> if (resize) draft.resized(dx, dy, minPx / wide, minPx / tall) else draft.moved(dx, dy)
                        }
                    }
                }
                val highlighted = arranging && selected == item.gridId
                // A compact widget has too little space for the three middle controls.
                // Float them beside its outline, keeping the strip inside the drawer.
                val compact = maxWidth * draft.w < 200.dp || maxHeight * draft.h < 96.dp
                if (highlighted && compact) {
                    val stripHeight = with(density) { 48.dp.toPx() }
                    val stripWidth = with(density) { 144.dp.toPx() }
                    val below = (draft.y + draft.h) * tall
                    PlacementMoveControls(
                        centerVertically = { draft = draft.copy(y = (1f - draft.h) / 2); commit() },
                        centerHorizontally = { draft = draft.copy(x = (1f - draft.w) / 2); commit() },
                        moveModifier = gesture(false), modifier = Modifier.zIndex(2f).absoluteOffset {
                            IntOffset((draft.x * wide).coerceIn(0f, (wide - stripWidth).coerceAtLeast(0f)).roundToInt(),
                                (if (below + stripHeight <= tall) below else (draft.y * tall - stripHeight).coerceAtLeast(0f)).roundToInt())
                        }.background(Color.DarkGray),
                    )
                }
                Box(Modifier.zIndex(if (highlighted || editingId == item.gridId) 1f else 0f)
                    .absoluteOffset { IntOffset((draft.x * wide).roundToInt(), (draft.y * tall).roundToInt()) }
                    .size(maxWidth * draft.w, maxHeight * draft.h)) {
                    WidgetItem(data = item, isDragging = false, currentWidgets = placed,
                        onWidgetsChanged = { changes ->
                            save(changes.map { changed ->
                                if (changed.gridId != item.gridId || changed.size == item.size) changed else
                                    changed.copy(drawerPlacement = saved.resized(
                                        (changed.safeSize.safeWidgetWidthSpan - item.safeSize.safeWidgetWidthSpan).toFloat() / columns,
                                        (changed.safeSize.safeWidgetHeightSpan - item.safeSize.safeWidgetHeightSpan).toFloat() / rows,
                                        minPx / wide, minPx / tall))
                            })
                        }, launchShortcutIconOverride = changeIcon, launchReconfigure = reconfigure,
                        resizeThresholdPx = { if (it == WidgetResizeListener.Which.LEFT || it == WidgetResizeListener.Which.RIGHT) wide / columns else tall / rows },
                        onCurrentEditingIdChanged = { currentEditingInterfaceId.value = it }, index = placed.indexOf(item),
                        columnCount = columns, rowCount = rows, currentEditingId = if (locked) null else editingId,
                        blockIndividualWidgetTouches = false)
                    if (arranging) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .16f))
                            .border(if (highlighted) 3.dp else 1.dp, Color.White).clickable { selected = item.gridId }.then(gesture(false)))
                        val resizeLabel = stringResource(R.string.drawer_resize_widget)
                        Box(Modifier.align(AbsoluteAlignment.BottomRight).size(40.dp).background(Color.DarkGray)
                            .semantics { contentDescription = resizeLabel }.then(gesture(true)), contentAlignment = Alignment.Center) {
                            Text("↘", color = Color.White)
                        }
                        if (highlighted) {
                            PlacementEdges { side -> gesture(true, side) }
                            if (!compact) {
                                PlacementMoveControls(
                                    centerVertically = { draft = draft.copy(y = (1f - draft.h) / 2); commit() },
                                    centerHorizontally = { draft = draft.copy(x = (1f - draft.w) / 2); commit() },
                                    moveModifier = gesture(false), modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            } }
        }
    }
    DisposableEffect(Unit) { onDispose { isResizingItem.value = false; globalState.itemIsActive[holderId] = false } }
}
