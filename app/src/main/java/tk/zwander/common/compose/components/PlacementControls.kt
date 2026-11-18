package tk.zwander.common.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tk.zwander.lockscreenwidgets.R

@Composable
internal fun PlacementMoveControls(
    centerVertically: () -> Unit, centerHorizontally: () -> Unit,
    moveModifier: Modifier, modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = centerVertically) {
            Icon(painterResource(R.drawable.ic_baseline_vertical_align_center_24), stringResource(R.string.center_vertically),
                Modifier.size(32.dp), tint = Color.White)
        }
        Icon(painterResource(R.drawable.ic_baseline_move_24), stringResource(R.string.move),
            moveModifier.size(32.dp), tint = Color.White)
        IconButton(onClick = centerHorizontally) {
            Icon(painterResource(R.drawable.ic_baseline_vertical_align_center_24), stringResource(R.string.center_horizontally),
                Modifier.rotate(90f).size(32.dp), tint = Color.White)
        }
    }
}

private data class EdgeControl(val icon: Int, val label: Int, val alignment: Alignment)

@Composable
internal fun BoxScope.PlacementEdges(gesture: @Composable (Side) -> Modifier) {
    for (side in Side.entries) {
        val (icon, label, alignment) = when (side) {
            Side.LEFT -> EdgeControl(R.drawable.handle_left, R.string.expand_left, AbsoluteAlignment.CenterLeft)
            Side.RIGHT -> EdgeControl(R.drawable.handle_right, R.string.expand_right, AbsoluteAlignment.CenterRight)
            Side.TOP -> EdgeControl(R.drawable.handle_top, R.string.expand_up, Alignment.TopCenter)
            Side.BOTTOM -> EdgeControl(R.drawable.handle_bottom, R.string.expand_down, Alignment.BottomCenter)
        }
        Icon(painterResource(icon), stringResource(label), Modifier.align(alignment).then(gesture(side)), tint = Color.White)
    }
}
