package tk.zwander.lockscreenwidgets.ambient

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tk.zwander.common.compose.util.widgetViewCacheRegistry
import tk.zwander.common.host.widgetHostCompat
import tk.zwander.common.util.PrefManager
import tk.zwander.common.util.keyguardManager
import tk.zwander.common.util.logUtils
import tk.zwander.common.util.prefManager
import java.util.Calendar

internal class AmbientDisplayController(
    private val service: AccessibilityService,
) : SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private val power = service.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val sensors = service.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximity = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val prefs = service.prefManager.prefs
    private val keyguard = service.keyguardManager

    private var sleepSession = false
    private var listening = false
    private var covered: Boolean? = null
    private var lastDisplayExit = -1000L
    private var activity: AmbientDisplayActivity? = null
    private var authenticationRequired = false
    private var widgetJob: Job? = null
    private var ambientWidget: AmbientWidget? = null
    private val host get() = service.widgetHostCompat
    private val refreshDisplay = Runnable { refresh() }
    private val launchTimedOut = Runnable {
        if (activity == null) endSession()
    }
    private val unlockCheck = object : Runnable {
        override fun run() {
            if (activity == null) return
            // Fingerprint unlock can clear authentication before the keyguard window goes away.
            val deviceLocked = keyguard.isDeviceLocked
            if (!keyguard.isKeyguardLocked || (authenticationRequired && !deviceLocked)) {
                endSession()
                return
            }
            authenticationRequired = authenticationRequired || deviceLocked
            handler.postDelayed(this, 200)
        }
    }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PrefManager.KEY_AMBIENT_WIDGET) endSession()
        if (key in setOf(AMBIENT_ENABLED, AMBIENT_QUIET_HOURS, AMBIENT_QUIET_START, AMBIENT_QUIET_END)) {
            refresh()
        }
    }

    private val notificationPrivacyListener = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // The power button can turn off the display while this activity is on top.
                    if (AmbientDisplayState.showing.value || SystemClock.uptimeMillis() - lastDisplayExit < 900L) {
                        endSession()
                        return
                    }
                    sleepSession = true
                    // Give the system time to finish the lock-screen transition.
                    handler.removeCallbacks(refreshDisplay)
                    handler.postDelayed(refreshDisplay, 300)
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (!AmbientDisplayState.showing.value) endSession()
                }
                Intent.ACTION_USER_PRESENT -> endSession()
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> refresh()
            }
        }
    }

    fun start(scope: CoroutineScope) {
        current = this
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        service.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(AmbientDisplayState.LOCK_SCREEN_NOTIFICATIONS),
            false,
            notificationPrivacyListener,
        )
        ContextCompat.registerReceiver(service, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        widgetJob = scope.launch {
            host.remoteViewsUpdates.collect { id ->
                if (ambientWidget?.id == id) refresh()
            }
        }
    }

    fun stop() {
        widgetJob?.cancel()
        service.unregisterReceiver(receiver)
        service.contentResolver.unregisterContentObserver(notificationPrivacyListener)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        endSession()
        current = null
    }

    fun attach(display: AmbientDisplayActivity): AmbientWidget? {
        if (!AmbientDisplayState.showing.value || !sleepSession || activity != null) return null
        val widget = ambientWidget ?: return null
        handler.removeCallbacks(launchTimedOut)
        activity = display
        authenticationRequired = keyguard.isDeviceLocked
        handler.post(unlockCheck)
        return widget
    }

    fun activityStopped(display: AmbientDisplayActivity) {
        if (activity !== display) return
        activity = null
        lastDisplayExit = SystemClock.uptimeMillis()
        endSession()
    }

    fun onNewNotification() {
        if (!sleepSession) return
        // Cancel a pending dim-screen launch as well as a visible display.
        lastDisplayExit = SystemClock.uptimeMillis()
        endSession()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!listening) return
        val distance = event.values.firstOrNull() ?: return
        if (!distance.isFinite() || distance < 0) return
        val near = distance < event.sensor.maximumRange
        if (covered != near) {
            covered = near
            refresh()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun refresh() {
        handler.removeCallbacks(refreshDisplay)
        if (!sleepSession) return
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val quiet = prefs.getBoolean(AMBIENT_QUIET_HOURS, false) && isInQuietHours(
            minutes,
            prefs.getInt(AMBIENT_QUIET_START, DEFAULT_QUIET_START),
            prefs.getInt(AMBIENT_QUIET_END, DEFAULT_QUIET_END),
        )
        if (!prefs.getBoolean(AMBIENT_ENABLED, false) || quiet) {
            stopDisplay(turnScreenOff = true)
            endSession()
            return
        }
        if (!listening && proximity != null) {
            listening = sensors.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL, handler)
        }
        val widget = ambientWidget ?: service.findAmbientWidget()?.also {
            ambientWidget = it
            try {
                service.widgetViewCacheRegistry.getOrCreateView(service, it.id, it.info, detach = false)
            } catch (e: RuntimeException) {
                service.logUtils.normalLog("Unable to load the dim-screen widget", e)
            }
        }
        val displayAllowed = (proximity == null || covered == false) &&
                widget != null && (!widget.isBadge || (
                    host.cachedRemoteViews[widget.id] != null &&
                    Settings.Secure.getInt(service.contentResolver, AmbientDisplayState.LOCK_SCREEN_NOTIFICATIONS, 1) != 0
                ))
        if (!displayAllowed) {
            stopDisplay(turnScreenOff = true)
        } else if (!AmbientDisplayState.showing.value && !power.isInteractive) {
            AmbientDisplayState.showing.value = true
            handler.postDelayed(launchTimedOut, 2500)
            try {
                service.startActivity(Intent(service, AmbientDisplayActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION,
                ))
            } catch (e: RuntimeException) {
                service.logUtils.normalLog("Unable to open the notification display", e)
                endSession()
            }
        }
        if (sleepSession && AmbientDisplayState.showing.value) {
            handler.postDelayed(refreshDisplay, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    private fun stopDisplay(turnScreenOff: Boolean) {
        handler.removeCallbacks(launchTimedOut)
        handler.removeCallbacks(unlockCheck)
        val display = activity
        activity = null
        val wasShowing = AmbientDisplayState.showing.value
        AmbientDisplayState.showing.value = false
        if (turnScreenOff && wasShowing && power.isInteractive && keyguard.isKeyguardLocked &&
            (!authenticationRequired || keyguard.isDeviceLocked)
        ) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
        display?.finish()
    }

    private fun endSession() {
        sleepSession = false
        handler.removeCallbacks(refreshDisplay)
        if (listening) sensors.unregisterListener(this)
        listening = false
        covered = null
        ambientWidget = null
        stopDisplay(turnScreenOff = false)
    }

    companion object {
        var current: AmbientDisplayController? = null
            private set
    }
}
