package tk.zwander.lockscreenwidgets.ambient

import android.app.TimePickerDialog
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tk.zwander.common.activities.BaseActivity
import tk.zwander.common.compose.util.rememberBooleanPreferenceState
import tk.zwander.common.compose.util.rememberIntPreferenceState
import tk.zwander.common.util.isAccessibilityEnabled
import tk.zwander.common.util.setThemedContent
import tk.zwander.lockscreenwidgets.R
import tk.zwander.widgetdrawer.activities.add.AddDrawerWidgetActivity
import java.util.Calendar

internal class AmbientSettingsActivity : BaseActivity() {
    private var hasBadgeWidget by mutableStateOf(false)
    private var hasAccessibilityAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        val hasProximity = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
        setThemedContent {
            val quietHours = rememberBooleanPreferenceState(AMBIENT_QUIET_HOURS)
            val start = rememberIntPreferenceState(AMBIENT_QUIET_START, DEFAULT_QUIET_START)
            val end = rememberIntPreferenceState(AMBIENT_QUIET_END, DEFAULT_QUIET_END)
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = { finish() }) { Text(stringResource(R.string.ambient_back)) }
                    Text(stringResource(R.string.ambient_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.ambient_details))
                    Text(stringResource(if (hasProximity) R.string.ambient_pocket else R.string.ambient_no_proximity))
                    if (!hasAccessibilityAccess) {
                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                            Text(stringResource(R.string.ambient_accessibility))
                        }
                    }
                    Text(stringResource(if (hasBadgeWidget) R.string.ambient_linked else R.string.ambient_missing_widget))
                    if (!hasBadgeWidget) {
                        Button(onClick = { AddDrawerWidgetActivity.launch(this@AmbientSettingsActivity, false) }) {
                            Text(stringResource(R.string.add_widget))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ListItem(
                        trailingContent = { Switch(quietHours.value, { quietHours.value = it }) },
                    ) { Text(stringResource(R.string.ambient_quiet_hours)) }
                    if (quietHours.value) {
                        TimeButton(R.string.ambient_quiet_start, start.value) {
                            start.value = it
                        }
                        TimeButton(R.string.ambient_quiet_end, end.value) {
                            end.value = it
                        }
                        if (start.value == end.value) Text(stringResource(R.string.ambient_quiet_all_day))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasBadgeWidget = findAmbientBadgeWidget() != null
        hasAccessibilityAccess = isAccessibilityEnabled
    }

    @Composable
    private fun TimeButton(label: Int, minutes: Int, update: (Int) -> Unit) {
        val hours = minutes / 60
        val minute = minutes % 60
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minute)
        }
        TextButton(onClick = {
            TimePickerDialog(
                this,
                { _, chosenHour, chosenMinute -> update(chosenHour * 60 + chosenMinute) },
                hours,
                minute,
                DateFormat.is24HourFormat(this),
            ).show()
        }) {
            Text(stringResource(label, DateFormat.getTimeFormat(this@AmbientSettingsActivity).format(time.time)))
        }
    }
}
