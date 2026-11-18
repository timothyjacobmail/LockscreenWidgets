package tk.zwander.widgetdrawer.activities

import android.annotation.SuppressLint
import android.view.Gravity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tk.zwander.common.compose.settings.BasePreference
import tk.zwander.common.compose.settings.ListPreference
import tk.zwander.common.compose.settings.ListPreferenceEntry
import tk.zwander.common.compose.settings.PreferenceScreenScope
import tk.zwander.common.compose.settings.SeekBarPreference
import tk.zwander.common.compose.util.rememberPreferenceState
import tk.zwander.common.util.PrefManager
import tk.zwander.common.util.lsDisplayManager
import tk.zwander.common.util.prefManager
import tk.zwander.lockscreenwidgets.R
import tk.zwander.widgetdrawer.util.HandlePosition
import tk.zwander.widgetdrawer.util.drawerHandleMaxY

internal class HandlePositionPreference : BasePreference<Int>(
    title = { stringResource(R.string.drawer_handle_position) },
    summary = { null },
    key = { PrefManager.KEY_DRAWER_HANDLE_Y_VALUE },
    icon = { null },
    defaultValue = { 0 },
) {
    @SuppressLint("RtlHardcoded")
    @Composable
    override fun PreferenceScreenScope.Render(modifier: Modifier) {
        val context = LocalContext.current
        val prefs = context.prefManager
        var side by rememberPreferenceState(
            key = PrefManager.KEY_DRAWER_HANDLE_SIDE,
            value = { prefs.drawerHandleSide },
            onChanged = { _, value -> prefs.drawerHandleSide = value },
        )
        var y by rememberPreferenceState(
            key = PrefManager.KEY_DRAWER_HANDLE_Y_VALUE,
            value = { prefs.drawerHandleYPosition },
            onChanged = { _, value -> prefs.drawerHandleYPosition = value },
        )
        val height by rememberPreferenceState(
            key = PrefManager.KEY_DRAWER_HANDLE_HEIGHT,
            value = { prefs.drawerHandleHeight },
        )
        val shown by rememberPreferenceState(
            key = PrefManager.KEY_SHOW_DRAWER_HANDLE,
            value = { prefs.showDrawerHandle },
        )
        val currentDisplay by context.lsDisplayManager.collectDisplay("0").collectAsState(null)
        val display = currentDisplay ?: context.lsDisplayManager.defaultDisplay
        val maxY = context.drawerHandleMaxY(display, height)

        Column(modifier) {
            ListPreference(
                title = stringResource(R.string.drawer_handle_edge),
                summary = stringResource(R.string.drawer_handle_edge_desc),
                entries = listOf(
                    ListPreferenceEntry(stringResource(R.string.drawer_handle_edge_left), Gravity.LEFT.toString()),
                    ListPreferenceEntry(stringResource(R.string.drawer_handle_edge_right), Gravity.RIGHT.toString()),
                ),
                currentValue = side.toString(),
                onValueSelected = { value -> value?.toIntOrNull()?.let { side = it } },
                icon = painterResource(R.drawable.expand_horiz),
                enabled = shown,
            )
            SeekBarPreference(
                title = stringResource(R.string.drawer_handle_vertical_position),
                summary = stringResource(R.string.drawer_handle_vertical_position_desc),
                value = HandlePosition.toPercent(y, maxY),
                onValueChanged = { y = HandlePosition.fromPercent(it, maxY) },
                defaultValue = 0,
                minValue = 0,
                maxValue = 100,
                unit = stringResource(R.string.unit_percent),
                icon = painterResource(R.drawable.expand_vert),
                enabled = shown && maxY > 0,
            )
        }
    }
}
